package reftree.util

import monocle.Lens

object Lenses {
  /** Tuple two lenses with a common source type */
  def tupleLensLeft[S, A, B](lensA: Lens[S, A], lensB: Lens[S, B]): Lens[S, (A, B)] =
    Lens[S, (A, B)](s ⇒ (lensA.get(s), lensB.get(s))) {
      case (a, b) ⇒ s ⇒ lensB.set(b)(lensA.set(a)(s))
    }

  /** Tuple three lenses with a common source type */
  def tupleLensLeft[S, A, B, C](lensA: Lens[S, A], lensB: Lens[S, B], lensC: Lens[S, C]): Lens[S, (A, B, C)] =
    Lens[S, (A, B, C)](s ⇒ (lensA.get(s), lensB.get(s), lensC.get(s))) {
      case (a, b, c) ⇒ s ⇒ lensC.set(c)(lensB.set(b)(lensA.set(a)(s)))
    }

  /** A view into the subset of elements of a sequence satisfying a predicate */
  def partitionLens[A](pred: A ⇒ Boolean) =
    Lens[Seq[A], Seq[A]](_.filter(pred)) { updated ⇒ sequence ⇒
      sequence.foldLeft((Vector.empty[A], updated)) {
        case ((acc, u), current) ⇒
          if (pred(current)) (acc :+ u.head, u.tail)
          else (acc :+ current, u)
      }._1
    }
}
