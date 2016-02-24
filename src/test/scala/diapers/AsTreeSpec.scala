package diapers

import org.scalatest.{Inside, Matchers, FlatSpec}
import AsTree._

case class Person(age: Int, name: String)

class AsTreeSpec extends FlatSpec with Matchers with Inside {
  it should "produce data mapping" in {
    val person = Person(3, "Nick")
    inside(person.tree) {
      case Tree.Ref(
        "Person", _,
        Seq(
          Tree.Val(3),
          Tree.Ref(
            "String", _,
            Seq(Tree.Val('N'), Tree.Val('i'), Tree.Val('c'), Tree.Val('k'))))) ⇒
    }
  }
}
