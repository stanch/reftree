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
  def apply(before: A, after: A, t: Double): A

  // TODO: override downstream for optimizations
  def sample(before: A, after: A, n: Int, inclusive: Boolean): Seq[A] = {
    val range = if (inclusive) {
      Seq.tabulate(n)(i ⇒ (i + 0.0) / (n - 1))
    } else {
      Seq.tabulate(if (inclusive) n - 2 else n)(i ⇒ (i + 1.0) / (n + 1))
    }
    range.map(self(before, after, _))
  }

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
