package reftree.geometry

import monocle.Iso

sealed trait PathSegment {
  import PathSegment._

  def to: Point

  def +(delta: Point): PathSegment = this match {
    case Move(to) ⇒ Move(to + delta)
    case Line(from, to) ⇒ Line(from + delta, to + delta)
    case Bezier(from, c1, c2, to) ⇒ Bezier(from + delta, c1 + delta, c2 + delta, to + delta)
  }

  def -(delta: Point): PathSegment = this match {
    case Move(to) ⇒ Move(to - delta)
    case Line(from, to) ⇒ Line(from - delta, to - delta)
    case Bezier(from, c1, c2, to) ⇒ Bezier(from - delta, c1 - delta, c2 - delta, to - delta)
  }

  def length: Double = this match {
    case m: Move ⇒ 0
    case Line(from, to) ⇒ from distance to
    case b: Bezier ⇒ toPolyline(4).length
  }

  def toPolyline(points: Int): Polyline = this match {
    case Move(to) ⇒ Polyline(Seq(to))
    case Line(from, to) ⇒
      Polyline(Point.interpolation.sample(from, to, points, inclusive = true))
    case Bezier(from, c1, c2, to) ⇒
      Polyline(Point.bezierInterpolation(c1, c2).sample(from, to, points, inclusive = true))
  }
}

object PathSegment {
  case class Move(to: Point) extends PathSegment {
    override def toString = s"M$to"
  }
  case class Line(from: Point, to: Point) extends PathSegment {
    override def toString = s"L$to"
  }
  case class Bezier(from: Point, c1: Point, c2: Point, to: Point) extends PathSegment {
    override def toString = s"C$c1, $c2, $to"
  }
}

case class Path(segments: Seq[PathSegment]) {
  def +(delta: Point) = copy(segments.map(_ + delta))
  def -(delta: Point) = copy(segments.map(_ - delta))

  def +(segment: PathSegment) = copy(segments :+ segment)
  def last = segments.last.to

  def simplify(points: Int) = {
    val lengths = segments.map(_.length)
    val totalLength = lengths.sum
    val pointDistribution = lengths.map(l ⇒ Math.max(points * l / totalLength, 1).toInt)
    val diff = pointDistribution.sum - points
    val compensatedPointDistribution = if (diff == 0) {
      pointDistribution
    } else {
      val (index, max) = pointDistribution.zipWithIndex.maxBy(_._2)
      pointDistribution.patch(index, Seq(Math.max(max + diff, 0)), 1)
    }
    (segments zip compensatedPointDistribution) map {
      case (e, p) ⇒ e.toPolyline(p)
    } reduceLeft (_ concatTailOf _)
  }

  override def toString = segments.mkString(" ")
}

object Path {
  def empty = Path(Seq.empty)

  def fromString(string: String) = parser.parse(string).get.value

  def fromPolyline(polyline: Polyline) = {
    val move = PathSegment.Move(polyline.points.head)
    val lines = polyline.points.sliding(2).toSeq map {
      case Seq(a, b) ⇒ PathSegment.Line(a, b)
    }
    Path(move +: lines)
  }

  private val parser = {
    import fastparse.all._

    val sep = P(" " | ",").rep(min = 1)
    val double = P(CharIn(('0' to '9') ++ Seq('-', '.')).rep(min = 1).!).map(_.toDouble)
    val point = P(double ~ sep ~ double).map { case (x, y) ⇒ Point(x, y) }

    def moveSegment(acc: Path): P[Path] = point
      .map(PathSegment.Move.apply)
      .map(acc + _)

    def lineSegment(acc: Path): P[Path] = point
      .map(PathSegment.Line(acc.last, _))
      .map(acc + _)

    def bezierSegment(acc: Path): P[Path] = (point ~ sep ~ point ~ sep ~ point)
      .map { case (c1, c2, to) ⇒ PathSegment.Bezier(acc.last, c1, c2, to) }
      .map(acc + _)

    def cycle(acc: Path, p: Path ⇒ P[Path]): P[Path] =
      p(acc).flatMap(acc2 ⇒ sep.? ~ cycle(acc2, p).?.map(_.getOrElse(acc2)))

    def move(acc: Path): P[Path] = P("M" ~/ cycle(acc, moveSegment))
    def line(acc: Path): P[Path] = P("L" ~/ cycle(acc, lineSegment))
    def bezier(acc: Path): P[Path] = P("C" ~/ cycle(acc, bezierSegment))

    cycle(Path.empty, acc ⇒ move(acc) | line(acc) | bezier(acc))
  }

  val interpolation = Polyline.interpolation.lensLeft {
    Iso[Path, Polyline](_.simplify(100))(fromPolyline).asLens
  }
}
