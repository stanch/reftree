package reftree.util

private[reftree] object Reflection {
  /** A utility for accessing private fields */
  implicit class PrivateFields[A](val value: A) extends AnyVal {
    def privateField[B](name: String): B = {
      val field = value.getClass.getDeclaredField(name)
      field.setAccessible(true)
      field.get(value).asInstanceOf[B]
    }

    def packagePrivateField[B](fieldName: String, className: String): B = {
      val cl = Class.forName(className)
      val field = cl.getDeclaredField(fieldName)
      field.setAccessible(true)
      field.get(value).asInstanceOf[B]
    }
  }
}
