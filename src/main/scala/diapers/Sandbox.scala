package diapers

import com.softwaremill.quicklens._
import AsData._

object Sandbox extends App {
  case class Street(name: String, house: Int)
  case class Address(street: Street, city: String)
  case class Person(address: Address, age: Int)

  val person = Person(Address(Street("Functional Rd.", 1), "London"), 35)

  println(AsciiPlotter.plot(
    person.asData,
    person.modify(_.address.street.house).using(_ + 3).asData
  ))
}
