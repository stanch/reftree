package reftree.geometry

import monocle.macros.GenLens

case class Point(x: Double, y: Double) {
  def +(delta: Point) = Point(x + delta.x, y + delta.y)
  def -(delta: Point) = Point(x - delta.x, y - delta.y)
  def *(factor: Double) = Point(x * factor, y * factor)

  def norm = Math.sqrt(x * x + y * y)
  def distance(that: Point) = (this - that).norm

  def topLeftMost(that: Point) = Point(this.x min that.x, this.y min that.y)
  def bottomRightMost(that: Point) = Point(this.x max that.x, this.y max that.y)

  override def toString = s"$x $y"
}

object Point {
  def zero = Point(0, 0)

  def sum(points: Seq[Point]) = points.foldLeft(zero)(_ + _)

  def fromString(string: String) = {
    val Array(x, y) = string.split(" |,").map(_.toDouble)
    Point(x, y)
  }

  val interpolation = Interpolation[Point]((b, a, t) ⇒ b * (1 - t) + a * t)

  def bezierInterpolation(c1: Point, c2: Point) = Interpolation[Point] { (b, a, t) ⇒
    b * Math.pow(1 - t, 3) +
    c1 * 3 * Math.pow(1 - t, 2) * t +
    c2   * 3 * (1 - t) * Math.pow(t, 2) +
    a * Math.pow(t, 3)
  }
}

case class Polyline(points: Seq[Point]) {
  def +(delta: Point) = copy(points.map(_ + delta))
  def -(delta: Point) = copy(points.map(_ - delta))

  def concatTailOf(that: Polyline) = Polyline(this.points ++ that.points.tail)

  def length = points.sliding(2).foldLeft(0.0) { case (l, Seq(a, b)) ⇒ l + (a distance b) }

  override def toString = points.mkString(",")
}

object Polyline {
  def fromString(string: String) = Polyline {
    string.split(" |,").map(_.toDouble).grouped(2).toSeq map {
      case Array(x, y) ⇒ Point(x, y)
    }
  }

  val interpolation = Interpolation.seq(Point.interpolation).lensBefore(GenLens[Polyline](_.points))
}

case class Rectangle(topLeft: Point, bottomRight: Point) {
  def width = bottomRight.x - topLeft.x
  def height = bottomRight.y - topLeft.y

  def +(delta: Point) = Rectangle(topLeft + delta, bottomRight + delta)

  def union(that: Rectangle) = Rectangle(
    this.topLeft topLeftMost that.topLeft,
    this.bottomRight bottomRightMost that.bottomRight
  )

  override def toString = s"$topLeft $width $height"
}

object Rectangle {
  def union(rectangles: Seq[Rectangle]) = rectangles.reduce(_ union _)

  def fromString(string: String) = {
    val Polyline(Seq(topLeft, widthHeight)) = Polyline.fromString(string)
    Rectangle(topLeft, topLeft + widthHeight)
  }
}
