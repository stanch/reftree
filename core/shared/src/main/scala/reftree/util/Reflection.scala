package reftree.util

object Reflection {
  /** A utility for accessing private fields */
  implicit class PrivateFields[A](value: A) {
    def privateField[B](name: String) = {
      val field = value.getClass.getDeclaredField(name)
      field.setAccessible(true)
      field.get(value).asInstanceOf[B]
    }
  }
}
