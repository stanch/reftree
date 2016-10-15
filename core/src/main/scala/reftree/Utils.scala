package reftree

object Utils {
  def iterate[A](start: A, n: Int)(iteration: A ⇒ A): Seq[A] =
    (1 to n).foldLeft(Vector(start))((current, _) ⇒ current :+ iteration(current.last))

  def flatIterate[A](start: A, n: Int)(iteration: A ⇒ Seq[A]): Seq[A] =
    (1 to n).foldLeft(Vector(start))((current, _) ⇒ current ++ iteration(current.last))

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
