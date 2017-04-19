package reftree.geometry

import monocle.{Lens, Prism, Optional}
import scala.collection.immutable.ListMap

/**
 * Semi-interpolation varies a single value over time.
 * It can be seen as an [[Interpolation]] with one end fixed (left or right).
 */
trait SemiInterpolation[A] { self ⇒
  /** Produce the value at time `t` (from 0 to 1) */
  def apply(value: A, t: Double): A

  /**
   * Map the time interval with the given function
   *
   * For example, the function (_ * 2) will make the interpolation twice faster,
   * and it will happen in the first half of the full time interval.
   */
  def mapTime(f: Double ⇒ Double) = SemiInterpolation[A] { (value, t) ⇒
    self(value, f(t) min 1 max 0)
  }

  /**
   * Make the interpolation happen in the given timespan, instead of [0; 1]
   *
   * This is essentially a more convenient alternative to `mapTime`.
   * For example, `timespan(0, 1.0/2)` is equivalent to `mapTime(_ * 2)`.
   */
  def timespan(from: Double, to: Double) = mapTime(t ⇒ (t - from) / (to - from))

  /**
   * Obtain a semi-interpolation of type `B` that varies a value of type `A`
   * “inside” it using the current semi-interpolation
   */
  def lens[B](l: Lens[B, A]) = SemiInterpolation[B] { (value, t) ⇒
    l.set(self(l.get(value), t))(value)
  }
}

object SemiInterpolation {
  def apply[A](f: (A, Double) ⇒ A): SemiInterpolation[A] = new SemiInterpolation[A] {
    def apply(value: A, t: Double) = f(value, t)
  }
}

/**
 * A trait for interpolating between two values of a certain type over time (from 0 to 1)
 */
trait Interpolation[A] { self ⇒
  def apply(left: A, right: A, t: Double): A

  /**
   * Sample the range between `left` and `right` using `n` values
   */
  def sample(left: A, right: A, n: Int, inclusive: Boolean = true): Seq[A] = {
    // TODO: consider overriding downstream for optimizations
    val range = if (inclusive) {
      Seq.tabulate(n)(i ⇒ (i + 0.0) / (n - 1))
    } else {
      Seq.tabulate(if (inclusive) n - 2 else n)(i ⇒ (i + 1.0) / (n + 1))
    }
    range.map(self(left, right, _))
  }

  /**
   * Map the time interval with the given function
   *
   * For example, the function (_ * 2) will make the interpolation twice faster,
   * and it will happen in the first half of the full time interval.
   */
  def mapTime(f: Double ⇒ Double) = Interpolation[A] { (left, right, t) ⇒
    self(left, right, f(t) min 1 max 0)
  }

  /**
   * Make the interpolation happen in the given timespan, instead of [0; 1]
   *
   * This is essentially a more convenient alternative to `mapTime`.
   * For example, `timespan(0, 1.0/2)` is equivalent to `mapTime(_ * 2)`.
   */
  def timespan(from: Double, to: Double) = mapTime(t ⇒ (t - from) / (to - from))

  /**
   * Combine with another interpolation of the same type
   *
   * Note that this operation is not commutative. It has a left bias,
   * i.e. the combined interpolations are applied from left to right,
   * always modifying the `left` argument.
   */
  def +(that: Interpolation[A]) = Interpolation[A] { (left, right, t) ⇒
    that(self(left, right, t), right, t)
  }

  /**
   * Obtain an interpolation of type `B` that varies a value of type `A`
   * “inside” it using the current interpolation
   *
   * Note that this operation is left-biased,
   * i.e. it applies the `set` function of the lens to the `left` argument.
   */
  def lens[B](l: Lens[B, A]): Interpolation[B] = Interpolation[B] { (left, right, t) ⇒
    l.set(self(l.get(left), l.get(right), t))(left)
  }

  /**
   * Obtain an interpolation of type `B` that varies a value of type `A`
   * “inside” it using the current interpolation
   *
   * Note that this operation is left-biased,
   * i.e. it applies the `set` function of the optional to the `left` argument.
   */
  def optional[B](o: Optional[B, A]): Interpolation[B] = Interpolation[B] { (left, right, t) ⇒
    (o.getOption(left), o.getOption(right)) match {
      case (Some(l), Some(r)) ⇒ o.set(self(l, r, t))(left)
      case _ ⇒ left
    }
  }

  /**
   * Obtain an interpolation of type `B` that varies a value of type `A`
   * “inside” it using the current interpolation
   *
   * Note that this operation is left-biased,
   * i.e. it applies the `set` function of the prism to the `left` argument.
   */
  def prism[B](p: Prism[B, A]): Interpolation[B] = optional(p.asOptional)

  /**
   * Derive a semi-interpolation by providing a function to calculate
   * the `left` argument from the `right` one
   */
  def withLeft(rightToLeft: A ⇒ A) = SemiInterpolation[A] { (value, t) ⇒
    self(rightToLeft(value), value, t)
  }

  /** Derive a semi-interpolation by using a fixed `left` argument */
  def withLeft(left: A) = SemiInterpolation[A]((value, t) ⇒ self(left, value, t))

  /**
   * Derive a semi-interpolation by providing a function to calculate
   * the `right` argument from the `left` one
   */
  def withRight(leftToRight: A ⇒ A) = SemiInterpolation[A] { (value, t) ⇒
    self(value, leftToRight(value), t)
  }

  /** Derive a semi-interpolation by using a fixed `right` argument */
  def withRight(right: A) = SemiInterpolation[A]((value, t) ⇒ self(value, right, t))

  /**
   * Derive an interpolation for `Option[A]` by providing semi-interpolations
   * for the remaining arguments when either `left` or `right` arguments are `None`
   */
  def option(
    leftOnly: SemiInterpolation[A],
    rightOnly: SemiInterpolation[A]
  ): Interpolation[Option[A]] = Interpolation[Option[A]] {
    case (Some(l), Some(r), t) ⇒ Some(self(l, r, t))
    case (Some(l), None, t) ⇒ Some(leftOnly(l, t))
    case (None, Some(r), t) ⇒ Some(rightOnly(r, t))
    case (None, None, t) ⇒ None
  }

  /**
   * Derive an interpolation for `Option[A]` by providing a function to calculate
   * the missing argument (i.e. None) from the remaining one (i.e. Some)
   */
  def option(default: A ⇒ A): Interpolation[Option[A]] =
    option(self.withRight(default), self.withLeft(default))

  /**
   * Derive an interpolation for `Option[A]` by providing a default
   * to use in place of a missing argument (i.e. None)
   */
  def option(default: A): Interpolation[Option[A]] =
    option(self.withRight(default), self.withLeft(default))

  /**
   * Derive an interpolation for a sequence (that assumes sequences of the same length)
   */
  def seq = Interpolation[Seq[A]] { (left, right, t) ⇒
    (left zip right) map {
      case (l, r) ⇒ self(l, r, t)
    }
  }

  /**
   * Derive an interpolation for a [[List]] (that assumes lists of the same length)
   */
  def list = Interpolation[List[A]] { (left, right, t) ⇒
    (left zip right) map {
      case (l, r) ⇒ self(l, r, t)
    }
  }

  /**
   * Derive an interpolation for a [[ListMap]]
   *
   * This method will compare the keys in both maps and use `Option[B]`
   * for `left` and `right` arguments associated with each key,
   * based on whether the key is only in the left map (Some, None),
   * only in the right one (None, Some), or in both (Some, Some).
   */
  def listMap[B](implicit evidence1: A =:= Option[B], evidence2: Option[B] =:= A) =
    Interpolation[ListMap[String, B]] { (left, right, t) ⇒
      val ids = (left.keysIterator ++ right.keysIterator).toSeq.distinct
      ListMap(ids flatMap { id ⇒
        self(left.get(id), right.get(id), t).map(id → _)
      }: _*)
    }
}

object Interpolation {
  /** A shorthand for constructing interpolations */
  def apply[A](f: (A, A, Double) ⇒ A): Interpolation[A] = new Interpolation[A] {
    def apply(left: A, right: A, t: Double): A = f(left, right, t)
  }

  /** A basic linear interpolation for doubles */
  val double = Interpolation[Double]((l, r, t) ⇒ l * (1 - t) + r * t)
}

/** Assorted syntax sugar */
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
