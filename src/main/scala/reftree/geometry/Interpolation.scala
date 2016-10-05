package reftree.geometry

import monocle.Lens
import scala.collection.immutable.ListMap

trait SemiInterpolation[A] { self ⇒
  def apply(value: A, t: Double): A
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

  def lensLeft[B](l: Lens[B, A]) = Interpolation[B] { (left, right, t) ⇒
    l.set(self(l.get(left), l.get(right), t))(left)
  }

  def lensRight[B](l: Lens[B, A]) = Interpolation[B] { (left, right, t) ⇒
    l.set(self(l.get(left), l.get(right), t))(right)
  }

  def withLeft(rightToLeft: A ⇒ A) = SemiInterpolation[A] { (value, t) ⇒
    self(rightToLeft(value), value, t)
  }

  def withRight(leftToRight: A ⇒ A) = SemiInterpolation[A] { (value, t) ⇒
    self(value, leftToRight(value), t)
  }
}

object Interpolation {
  def apply[A](f: (A, A, Double) ⇒ A): Interpolation[A] = new Interpolation[A] {
    def apply(left: A, right: A, t: Double): A = f(left, right, t)
  }

  val double = Interpolation[Double]((l, r, t) ⇒ l * (1 - t) + r * t)

  def combineLeft[A](interpolation0: Interpolation[A], interpolations: Interpolation[A]*) =
    Interpolation[A] { (left, right, t) ⇒
      interpolations.foldLeft(interpolation0(left, right, t))((l, i) ⇒ i(l, right, t))
    }

  def combineRight[A](interpolation0: Interpolation[A], interpolations: Interpolation[A]*) =
    Interpolation[A] { (left, right, t) ⇒
      interpolations.foldRight(interpolation0(left, right, t))((i, r) ⇒ i(left, r, t))
    }

  def option[A](
    leftOnly: SemiInterpolation[A],
    rightOnly: SemiInterpolation[A],
    both: Interpolation[A]
  ) = Interpolation[Option[A]] {
    case (Some(l), Some(r), t) ⇒ Some(both(l, r, t))
    case (Some(l), None, t) ⇒ Some(leftOnly(l, t))
    case (None, Some(r), t) ⇒ Some(rightOnly(r, t))
    case (None, None, t) ⇒ None
  }

  def seq[A](interpolation: Interpolation[A]) =
    Interpolation[Seq[A]] { (left, right, t) ⇒
      (left zip right) map {
        case (l, r) ⇒ interpolation(l, r, t)
      }
    }

  def map[A](optionInterpolation: Interpolation[Option[A]]) =
    Interpolation[ListMap[String, A]] { (left, right, t) ⇒
      val ids = (left.keys ++ right.keys).toSeq.distinct
      ListMap(ids flatMap { id ⇒
        optionInterpolation(left.get(id), right.get(id), t).map(id → _)
      }: _*)
    }
}
