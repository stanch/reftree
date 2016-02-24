package diapers

import com.softwaremill.quicklens._
import AsData._

object Sandbox extends App {
  case class Street(name: String, house: Int)
  case class Address(street: Street, city: String)
  case class Person(address: Address, age: Int)

  val person1 = Person(Address(Street("Functional Rd.", 1), "London"), 35)
  val person2 = person1.modify(_.address.street.house).using(_ + 3)

  val list1 = List(1, 2, 3, 4, 5)
  val list2 = List(-1, -2) ++ list1.drop(2)

  println(AsciiPlotter.plot(
    person1.asData,
    person2.asData
  ))

  println(DotPlotter.plot(
    list1.asData,
    list2.asData,
    person1.asData,
    person2.asData
  ))
}
