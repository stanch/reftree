package reftree

object Utils {
  def iterate[A](start: A, n: Int)(iteration: A ⇒ A): Seq[A] =
    Iterator.iterate(start)(iteration).take(n).toSeq

  def flatIterate[A](start: A, n: Int)(iteration: A ⇒ Seq[A]): Seq[A] =
    if (n == 0) Seq.empty else {
      iteration(start) match {
        case seq @ (_ :+ last) ⇒ seq ++ flatIterate(last, n - 1)(iteration)
        case _ ⇒ Seq.empty
      }
    }

  def iterate[A](start: A)(iterations: (A ⇒ A)*): Seq[A] =
    iterations.foldLeft(Vector(start))((current, iteration) ⇒ current :+ iteration(current.last))

  def flatIterate[A](start: A)(iterations: (A ⇒ Seq[A])*): Seq[A] =
    iterations.foldLeft(Vector(start))((current, iteration) ⇒ current ++ iteration(current.last))

  implicit class PrivateFields[A](value: A) {
    def privateField[B](name: String) = {
      val field = value.getClass.getDeclaredField(name)
      field.setAccessible(true)
      field.get(value).asInstanceOf[B]
    }
  }
}
