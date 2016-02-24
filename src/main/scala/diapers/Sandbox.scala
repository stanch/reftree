package diapers

import com.softwaremill.quicklens._
import AsTree._

import scala.collection.immutable.HashSet

object Sandbox extends App {
  case class Street(name: String, house: Int)
  case class Address(street: Street, city: String)
  case class Person(address: Address, age: Int)

  val person1 = Person(Address(Street("Functional Rd.", 1), "London"), 35)
  val person2 = person1.modify(_.address.street.house).using(_ + 3)

  val list1 = List(1, 2, 3, 4, 5)
  val list2 = List(-1, -2) ++ list1.drop(2)

  val set1 = HashSet(1 to 33 :_*)
  val set2 = HashSet(1L, 2L + 2L * Int.MaxValue, 3L, 4L)

  println(AsciiPlotter.plot(
    person1.tree,
    person2.tree
  ))

  println(DotPlotter.plot(
    list1.tree,
    list2.tree,
    person1.tree,
    person2.tree
  ))
}
