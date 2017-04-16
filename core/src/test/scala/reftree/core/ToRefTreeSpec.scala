package reftree.core

import org.scalatest.{FlatSpec, Matchers}

case class Person(age: Int, name: String)

class ToRefTreeSpec extends FlatSpec with Matchers {
  it should "auto-derive ToRefTree instances" in {
    Person(3, "Nick").refTree should matchPattern {
      case RefTree.Ref(
        "Person", _,
        Seq(
          RefTree.Ref.Field(RefTree.Val(3, None, false), Some("age"), false),
          RefTree.Ref.Field(RefTree.Ref(
            "String", _,
            Seq(
              RefTree.Ref.Field(RefTree.Val('N', None, false), None, false),
              RefTree.Ref.Field(RefTree.Val('i', None, false), None, false),
              RefTree.Ref.Field(RefTree.Val('c', None, false), None, false),
              RefTree.Ref.Field(RefTree.Val('k', None, false), None, false)
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
          RefTree.Ref.Field(RefTree.Val(3, None, true), Some("years"), false)
        ),
        false
      ) ⇒
    }
  }
}
