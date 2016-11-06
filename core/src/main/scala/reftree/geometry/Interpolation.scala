package reftree.geometry

import monocle.{Lens, Prism, Optional}
import scala.collection.immutable.ListMap

trait SemiInterpolation[A] { self ⇒
  def apply(value: A, t: Double): A

  def mapTime(f: Double ⇒ Double) = SemiInterpolation[A] { (value, t) ⇒
    self(value, f(t) min 1 max 0)
  }

  def timespan(from: Double, to: Double) = mapTime(t ⇒ (t - from) / (to - from))

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
  def apply(left: A, right: A, t: Double): A

  // TODO: override downstream for optimizations
  def sample(left: A, right: A, n: Int, inclusive: Boolean): Seq[A] = {
    val range = if (inclusive) {
      Seq.tabulate(n)(i ⇒ (i + 0.0) / (n - 1))
    } else {
      Seq.tabulate(if (inclusive) n - 2 else n)(i ⇒ (i + 1.0) / (n + 1))
    }
    range.map(self(left, right, _))
  }

  def mapTime(f: Double ⇒ Double) = Interpolation[A] { (left, right, t) ⇒
    self(left, right, f(t) min 1 max 0)
  }

  def timespan(from: Double, to: Double) = mapTime(t ⇒ (t - from) / (to - from))

  def +(that: Interpolation[A]) = Interpolation[A] { (left, right, t) ⇒
    that(self(left, right, t), right, t)
  }

  def lens[B](l: Lens[B, A]): Interpolation[B] = Interpolation[B] { (left, right, t) ⇒
    l.set(self(l.get(left), l.get(right), t))(left)
  }

  def optional[B](o: Optional[B, A]): Interpolation[B] = Interpolation[B] { (left, right, t) ⇒
    (o.getOption(left), o.getOption(right)) match {
      case (Some(l), Some(r)) ⇒ o.set(self(l, r, t))(left)
      case _ ⇒ left
    }
  }

  def prism[B](p: Prism[B, A]): Interpolation[B] = optional(p.asOptional)

  def withLeft(rightToLeft: A ⇒ A) = SemiInterpolation[A] { (value, t) ⇒
    self(rightToLeft(value), value, t)
  }

  def withLeft(left: A) = SemiInterpolation[A]((value, t) ⇒ self(left, value, t))

  def withRight(leftToRight: A ⇒ A) = SemiInterpolation[A] { (value, t) ⇒
    self(value, leftToRight(value), t)
  }

  def withRight(right: A) = SemiInterpolation[A]((value, t) ⇒ self(value, right, t))

  def option(
    leftOnly: SemiInterpolation[A],
    rightOnly: SemiInterpolation[A]
  ): Interpolation[Option[A]] = Interpolation[Option[A]] {
    case (Some(l), Some(r), t) ⇒ Some(self(l, r, t))
    case (Some(l), None, t) ⇒ Some(leftOnly(l, t))
    case (None, Some(r), t) ⇒ Some(rightOnly(r, t))
    case (None, None, t) ⇒ None
  }

  def option(default: A ⇒ A): Interpolation[Option[A]] =
    option(self.withRight(default), self.withLeft(default))

  def option(default: A): Interpolation[Option[A]] =
    option(self.withRight(default), self.withLeft(default))

  def seq = Interpolation[Seq[A]] { (left, right, t) ⇒
    (left zip right) map {
      case (l, r) ⇒ self(l, r, t)
    }
  }

  def list = Interpolation[List[A]] { (left, right, t) ⇒
    (left zip right) map {
      case (l, r) ⇒ self(l, r, t)
    }
  }

  def listMap[B](implicit evidence1: A =:= Option[B], evidence2: Option[B] =:= A) =
    Interpolation[ListMap[String, B]] { (left, right, t) ⇒
      val ids = (left.keys ++ right.keys).toSeq.distinct
      ListMap(ids flatMap { id ⇒
        self(left.get(id), right.get(id), t).map(id → _)
      }: _*)
    }
}

object Interpolation {
  def apply[A](f: (A, A, Double) ⇒ A): Interpolation[A] = new Interpolation[A] {
    def apply(left: A, right: A, t: Double): A = f(left, right, t)
  }

  val double = Interpolation[Double]((l, r, t) ⇒ l * (1 - t) + r * t)
}

trait InterpolationSyntax {
  implicit class LensInterpolation[A, B](l: Lens[B, A]) {
    def interpolateWith(interpolation: Interpolation[A]) = interpolation.lens(l)
    def semiInterpolateWith(semiInterpolation: SemiInterpolation[A]) = semiInterpolation.lens(l)
  }

  implicit class LensSeqInterpolation[A, B](l: Lens[B, Seq[A]]) {
    def interpolateEachWith(interpolation: Interpolation[A]) = interpolation.seq.lens(l)
  }

  implicit class LensListInterpolation[A, B](l: Lens[B, List[A]]) {
    def interpolateEachWith(interpolation: Interpolation[A]) = interpolation.list.lens(l)
  }

  implicit class LensListMapInterpolation[A, B](l: Lens[B, ListMap[String, A]]) {
    def interpolateEachWith(interpolation: Interpolation[Option[A]]) = interpolation.listMap[A].lens(l)
  }

  implicit class OptionalInterpolation[A, B](o: Optional[B, A]) {
    def interpolateWith(interpolation: Interpolation[A]) = interpolation.optional(o)
  }

  implicit class OptionalListInterpolation[A, B](o: Optional[B, List[A]]) {
    def interpolateEachWith(interpolation: Interpolation[A]) = interpolation.list.optional(o)
  }

  implicit class OptionalListMapInterpolation[A, B](o: Optional[B, ListMap[String, A]]) {
    def interpolateEachWith(interpolation: Interpolation[Option[A]]) = interpolation.listMap[A].optional(o)
  }

  implicit class PrismInterpolation[A, B](p: Prism[B, A]) {
    def interpolateWith(interpolation: Interpolation[A]) = interpolation.prism(p)
  }
}
