package diapers

import org.scalatest.{Inside, Matchers, FlatSpec}
import AsData._

case class Person(age: Int, name: String)

class AsDataSpec extends FlatSpec with Matchers with Inside {
  it should "produce data mapping" in {
    val person = Person(3, "Nick")
    inside(person.asData) {
      case Data.Ref(
        "Person", _,
        Seq(
          Data.Val(3),
          Data.Ref(
            "String", _,
            Seq(Data.Val('N'), Data.Val('i'), Data.Val('c'), Data.Val('k'))))) ⇒
    }
  }
}
