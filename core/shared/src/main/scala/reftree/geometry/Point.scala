package reftree.geometry

import monocle.Iso
import monocle.macros.GenLens

import scala.annotation.implicitNotFound

/** A typeclass for values that can be translated */
@implicitNotFound("Could not find a way to translate a value of type ${A}")
trait Translatable[A] {
  def translate(value: A, delta: Point): A
}

object Translatable {
  def apply[A](f: (A, Point) ⇒ A): Translatable[A] = new Translatable[A] {
    def translate(value: A, delta: Point) = f(value, delta)
  }

  implicit def `List Translatable`[A](implicit t: Translatable[A]): Translatable[List[A]] =
    Translatable((value, delta) ⇒ value.map(t.translate(_, delta)))
}

/** A point on a plane */
case class Point(x: Double, y: Double) {
  def +(delta: Point) = Point(x + delta.x, y + delta.y)
  def -(delta: Point) = Point(x - delta.x, y - delta.y)
  def *(factor: Double) = Point(x * factor, y * factor)
  def unary_- = Point(-x, -y)

  def norm = Math.sqrt(x * x + y * y)
  def distance(that: Point) = (this - that).norm

  def topLeftMost(that: Point) = Point(this.x min that.x, this.y min that.y)
  def bottomRightMost(that: Point) = Point(this.x max that.x, this.y max that.y)

  override def toString = s"$x $y"
}

object Point {
  val zero = Point(0, 0)

  def sum(points: Seq[Point]) = points.foldLeft(zero)(_ + _)

  def mean(points: Seq[Point]) = sum(points) * (1.0 / points.length)

  /** Parse an SVG point */
  def fromString(string: String) = {
    val Array(x, y) = string.split(" |,").map(_.toDouble)
    Point(x, y)
  }

  /** An isomorphism between (x, y) string pairs [[Point]] */
  val stringPairIso: Iso[(String, String), Point] =
    Iso[(String, String), Point] {
      case (x, y) ⇒ Point(x.toDouble, y.toDouble)
    } { point ⇒
      (point.x.toString, point.y.toString)
    }

  val interpolation = Interpolation[Point]((l, r, t) ⇒ l * (1 - t) + r * t)

  /** Interpolate between two points on a cubic Bezier curve */
  def bezierInterpolation(c1: Point, c2: Point) = Interpolation[Point] { (l, r, t) ⇒
    l * Math.pow(1 - t, 3) +
    c1 * 3 * Math.pow(1 - t, 2) * t +
    c2 * 3 * Math.pow(t, 2) * (1 - t) +
    r * Math.pow(t, 3)
  }

  implicit val `Point Translatable`: Translatable[Point] =
    Translatable(_ + _)
}

/** A polyline is a sequence of points */
case class Polyline(points: Seq[Point]) {
  def +(delta: Point) = copy(points.map(_ + delta))

  def concatTailOf(that: Polyline) = Polyline(this.points ++ that.points.tail)

  def length = points.sliding(2).foldLeft(0.0) { case (l, Seq(a, b)) ⇒ l + (a distance b) }

  override def toString = points.mkString(",")
}

object Polyline {
  /** Parse an SVG polyline */
  def fromString(string: String) = Polyline {
    string.split(" |,").map(_.toDouble).grouped(2).toSeq map {
      case Array(x, y) ⇒ Point(x, y)
    }
  }

  /** An isomorphism between SVG polylines and [[Polyline]] */
  val stringIso: Iso[String, Polyline] =
    Iso[String, Polyline](fromString)(_.toString)

  val interpolation = GenLens[Polyline](_.points)
    .interpolateEachWith(Point.interpolation)

  implicit val `Polyline Translatable`: Translatable[Polyline] =
    Translatable(_ + _)
}

/** A rectangle defined by its top-left and bottom-right corners */
case class Rectangle(topLeft: Point, bottomRight: Point) {
  def width = bottomRight.x - topLeft.x
  def height = bottomRight.y - topLeft.y

  def +(delta: Point) = Rectangle(topLeft + delta, bottomRight + delta)

  /** Calculate the rectangle enclosing both this and that */
  def union(that: Rectangle) = Rectangle(
    this.topLeft topLeftMost that.topLeft,
    this.bottomRight bottomRightMost that.bottomRight
  )

  override def toString = s"$topLeft $width $height"
}

object Rectangle {
  /** Calculate the rectangle enclosing all the provided ones */
  def union(rectangles: Seq[Rectangle]) = rectangles.reduce(_ union _)

  /** Parse an SVG rectangle */
  def fromString(string: String) = {
    val Polyline(Seq(topLeft, widthHeight)) = Polyline.fromString(string)
    Rectangle(topLeft, topLeft + widthHeight)
  }

  implicit val `Rectangle Translatable`: Translatable[Rectangle] =
    Translatable(_ + _)
}
