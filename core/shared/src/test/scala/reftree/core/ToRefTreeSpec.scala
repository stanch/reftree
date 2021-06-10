package reftree.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

case class Person(age: Int, name: String)

class ToRefTreeSpec extends AnyFlatSpec with Matchers {
  it should "auto-derive ToRefTree instances" in {
    Person(3, "Nick").refTree should matchPattern {
      case RefTree.Ref(
        "Person", _,
        Seq(
          RefTree.Ref.Field(RefTree.Val(3, "3", false), Some("age"), false),
          RefTree.Ref.Field(RefTree.Ref(
            "String", _,
            Seq(
              RefTree.Ref.Field(RefTree.Val('N', "N", false), None, false),
              RefTree.Ref.Field(RefTree.Val('i', "i", false), None, false),
              RefTree.Ref.Field(RefTree.Val('c', "c", false), None, false),
              RefTree.Ref.Field(RefTree.Val('k', "k", false), None, false)
            ),
            false
          ), Some("name"), false)
        ),
        false
      ) ⇒
    }
  }

  it should "allow to configure automatic derivation" in {
    implicit val personDerivationConfig = ToRefTree.DerivationConfig[Person]
      .renameWith(_.name)
      .omitField("name")
      .tweakField("age", _.withName("years").withTreeHighlight(true))

    Person(3, "Nick").refTree should matchPattern {
      case RefTree.Ref(
        "Nick", _,
        Seq(
          RefTree.Ref.Field(RefTree.Val(3, "3", true), Some("years"), false)
        ),
        false
      ) ⇒
    }
  }
}
