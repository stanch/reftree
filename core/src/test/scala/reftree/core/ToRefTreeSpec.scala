package reftree.core

import org.scalatest.{FlatSpec, Matchers}

case class Person(age: Int, name: String)

class ToRefTreeSpec extends FlatSpec with Matchers {
  it should "produce data mapping" in {
    val person = Person(3, "Nick")
    person.refTree should matchPattern {
      case RefTree.Ref(
        "Person", _,
        Seq(
          RefTree.Ref.Field(RefTree.Val(3, None, false, false), Some("age")),
          RefTree.Ref.Field(RefTree.Ref(
            "String", _,
            Seq(
              RefTree.Ref.Field(RefTree.Val('N', None, false, false), None),
              RefTree.Ref.Field(RefTree.Val('i', None, false, false), None),
              RefTree.Ref.Field(RefTree.Val('c', None, false, false), None),
              RefTree.Ref.Field(RefTree.Val('k', None, false, false), None)
            ),
            false,
            false
          ), Some("name"))
        ),
        false,
        false
      ) ⇒
    }
  }
}
