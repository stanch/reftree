package reftree.geometry

import monocle.Iso

/** A segment of an SVG-like path */
sealed trait PathSegment {
  import PathSegment._

  /** The final point of the segment */
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

  /** Estimate the length of this segment */
  def length: Double = this match {
    case m: Move ⇒ 0
    case Line(from, to) ⇒ from distance to
    case b: Bezier ⇒ toPolyline(4).length // for splines we do a crude estimation
  }

  /** Approximate the segment with a polyline using the given number of points */
  def toPolyline(points: Int): Polyline = this match {
    case Move(to) ⇒ Polyline(Seq(to))
    case Line(from, to) ⇒
      // Even though 2 points is enough, we want to use all the points,
      // so that this line can morph into a different shape uniformly.
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

/** A path defined by a sequence of linear or Bezier segments, modeled after SVG paths */
case class Path(segments: Seq[PathSegment]) {
  def +(delta: Point) = copy(segments.map(_ + delta))

  def +(segment: PathSegment) = copy(segments :+ segment)
  def last = segments.last.to

  /** Approximate the path with a polyline, using the specified number of points */
  def simplify(points: Int) = {
    // first, estimate the lengths of the segments
    val lengths = segments.map(_.length)
    val totalLength = lengths.sum
    // distribute the points proportionally to segment lengths
    val pointDistribution = lengths.map(l ⇒ Math.max(points * l / totalLength, 1).toInt)
    val diff = pointDistribution.sum - points
    // we might be off by a few points...
    val compensatedPointDistribution = if (diff == 0) {
      // lucky!
      pointDistribution
    } else {
      // let’s add/remove the extra points to/from the segment that has the most
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

  /** Parse an SVG path */
  def fromString(string: String) = parser.parse(string).get.value

  /** An isomorphism between SVG paths and [[Path]] */
  val stringIso: Iso[String, Path] =
    Iso[String, Path](fromString)(_.toString)

  /** Convert a polyline into a path */
  def fromPolyline(polyline: Polyline) = {
    val move = PathSegment.Move(polyline.points.head)
    val lines = polyline.points.sliding(2).toSeq map {
      case Seq(a, b) ⇒ PathSegment.Line(a, b)
    }
    Path(move +: lines)
  }

  /** An (imprecise) isomorphism between [[Path]] and [[Polyline]] */
  def polylineIso(points: Int): Iso[Path, Polyline] =
    Iso[Path, Polyline](_.simplify(points))(fromPolyline)

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

  /** Interpolate via a polyline approximation */
  def interpolation(points: Int) = polylineIso(points).asLens
    .interpolateWith(Polyline.interpolation)

  implicit val `Path Translatable`: Translatable[Path] =
    Translatable(_ + _)
}
