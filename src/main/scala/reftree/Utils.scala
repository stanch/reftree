package reftree

object Utils {
  def iterate[A](start: A, n: Int)(iteration: A ⇒ A): Seq[A] =
    Iterator.iterate(start)(iteration).take(n).toSeq

  def iterate[A](start: A)(iterations: (A ⇒ A)*): Seq[A] =
    iterations.foldLeft(Vector(start))((current, iteration) ⇒ current :+ iteration(current.last))
}
