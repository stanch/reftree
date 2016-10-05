package reftree

import org.scalatest.{Inside, Matchers, FlatSpec}
import ToRefTree._

case class Person(age: Int, name: String)

class ToRefTreeSpec extends FlatSpec with Matchers {
  it should "produce data mapping" in {
    val person = Person(3, "Nick")
    person.refTree should matchPattern {
      case RefTree.Ref(
        "Person", _,
        Seq(
          RefTree.Val(3, None, false),
          RefTree.Ref(
            "String", _,
            Seq(
              RefTree.Val('N', None, false),
              RefTree.Val('i', None, false),
              RefTree.Val('c', None, false),
              RefTree.Val('k', None, false)
            ),
            false
          )
        ),
        false
      ) ⇒
    }
  }
}
