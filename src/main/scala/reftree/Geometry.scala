package reftree

import monocle.Lens

import scala.collection.immutable.ListMap

object Geometry {
  case class Point(x: Double, y: Double) {
    def +(delta: Point) = Point(x + delta.x, y + delta.y)
    def -(delta: Point) = Point(x - delta.x, y - delta.y)
    def *(factor: Double) = Point(x * factor, y * factor)

    def topLeftMost(that: Point) = Point(this.x min that.x, this.y min that.y)
    def bottomRightMost(that: Point) = Point(this.x max that.x, this.y max that.y)

    def commaSeparatedString = s"$x,$y"
    override def toString = s"$x $y"
  }

  object Point {
    def zero = Point(0, 0)

    def sum(points: Seq[Point]) = points.foldLeft(zero)(_ + _)

    def fromString(string: String, sep: String) = {
      val Array(x, y) = string.split(sep).map(_.toDouble)
      Point(x, y)
    }

    def sequenceFromString(string: String, sep1: String, sep2: String) =
      string.split(sep1).flatMap(_.split(sep2)).map(_.toDouble).grouped(2).toSeq map {
        case Array(x, y) ⇒ Point(x, y)
      }
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

    def fromString(string: String, sep1: String, sep2: String) = {
      val points = Point.sequenceFromString(string, sep1, sep2)
      Rectangle(points.head, points.head + points.last)
    }
  }

  trait SemiInterpolation[A] { self ⇒
    def apply(value: A, t: Double): A

    def lens[B](l: Lens[B, A]) = SemiInterpolation[B] { (value, t) ⇒
      l.set(self(l.get(value), t))(value)
    }
  }

  object SemiInterpolation {
    def apply[A](f: (A, Double) ⇒ A): SemiInterpolation[A] = new SemiInterpolation[A] {
      def apply(value: A, t: Double) = f(value, t)
    }
  }

  trait Interpolation[A] { self ⇒
    def apply(before: A, after: A, t: Double): A

    def mapTime(f: Double ⇒ Double) = Interpolation[A] { (before, after, t) ⇒
      self(before, after, f(t) min 1 max 0)
    }

    def lensBefore[B](l: Lens[B, A]) = Interpolation[B] { (before, after, t) ⇒
      l.set(self(l.get(before), l.get(after), t))(before)
    }

    def lensAfter[B](l: Lens[B, A]) = Interpolation[B] { (before, after, t) ⇒
      l.set(self(l.get(before), l.get(after), t))(after)
    }

    def withBefore(afterToBefore: A ⇒ A) = SemiInterpolation[A] { (value, t) ⇒
      self(afterToBefore(value), value, t)
    }

    def withAfter(beforeToAfter: A ⇒ A) = SemiInterpolation[A] { (value, t) ⇒
      self(value, beforeToAfter(value), t)
    }
  }

  object Interpolation {
    def apply[A](f: (A, A, Double) ⇒ A): Interpolation[A] = new Interpolation[A] {
      def apply(before: A, after: A, t: Double): A = f(before, after, t)
    }

    val double = Interpolation[Double]((b, a, t) ⇒ b * (1 - t) + a * t)
    val point = Interpolation[Point]((b, a, t) ⇒ b * (1 - t) + a * t)

    def foldLeftBefore[A](interpolation0: Interpolation[A], interpolations: Interpolation[A]*) =
      Interpolation[A] { (before, after, t) ⇒
        interpolations.foldLeft(interpolation0(before, after, t))((b, i) ⇒ i(b, after, t))
      }

    def foldRightAfter[A](interpolation0: Interpolation[A], interpolations: Interpolation[A]*) =
      Interpolation[A] { (before, after, t) ⇒
        interpolations.foldRight(interpolation0(before, after, t))((i, a) ⇒ i(before, a, t))
      }

    def option[A](
      beforeOnly: SemiInterpolation[A],
      afterOnly: SemiInterpolation[A],
      both: Interpolation[A]
    ) = Interpolation[Option[A]] {
      case (Some(b), Some(a), t) ⇒ Some(both(b, a, t))
      case (Some(b), None, t) ⇒ Some(beforeOnly(b, t))
      case (None, Some(a), t) ⇒ Some(afterOnly(a, t))
      case (None, None, t) ⇒ None
    }

    def seq[A](interpolation: Interpolation[A]) =
      Interpolation[Seq[A]] { (before, after, t) ⇒
        (before zip after) map {
          case (b, a) ⇒ interpolation(b, a, t)
        }
      }

    def map[A](optionInterpolation: Interpolation[Option[A]]) =
      Interpolation[ListMap[String, A]] { (before, after, t) ⇒
        val ids = (before.keys ++ after.keys).toSeq.distinct
        ListMap(ids flatMap { id ⇒
          optionInterpolation(before.get(id), after.get(id), t).map(id → _)
        }: _*)
      }
  }
}
