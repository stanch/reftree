package diapers

import shapeless.{Lazy, HList, HNil, Generic}

sealed trait Tree

object Tree {
  case class Val(value: AnyVal) extends Tree

  case class Ref(name: String, id: String, children: Seq[Tree]) extends Tree
  object Ref {
    def apply(value: AnyRef, children: Seq[Tree]): Ref =
      Ref(
        value.getClass.getSimpleName,
        s"${value.getClass.getName.replace("$", "").replace(".", "")}${System.identityHashCode(value)}",
        children
      )
  }
}

trait AsTree[-A] {
  def tree(value: A): Tree
}

object AsTree {
  implicit class AsTreeOps[A: AsTree](value: A) {
    def tree = implicitly[AsTree[A]].tree(value)
  }

  implicit def `AnyVal as Tree`: AsTree[AnyVal] = new AsTree[AnyVal] {
    def tree(value: AnyVal) = Tree.Val(value)
  }

  implicit def `String as Tree`: AsTree[String] = new AsTree[String] {
    def tree(value: String) = Tree.Ref(value, value.map(Tree.Val))
  }

  implicit def `List as Tree`[A: AsTree]: AsTree[List[A]] = new AsTree[List[A]] {
    def tree(value: List[A]): Tree = value match {
      case head :: tail ⇒ Tree.Ref(value, Seq(head.tree, tree(tail))).copy(name = "Cons")
      case Nil ⇒ Tree.Ref(Nil, Seq.empty).copy(name = "Nil")
    }
  }

  import shapeless.::

  implicit val `HNil as Tree`: AsTree[HNil] = new AsTree[HNil] {
    def tree(value: HNil) = Tree.Ref(value, Seq.empty)
  }

  implicit def `HCons as Tree`[H: AsTree, T <: HList: AsTree]: AsTree[H :: T] = new AsTree[H :: T] {
    def tree(value: H :: T): Tree = Tree.Ref(value, value.head.tree +: (value.tail.tree match {
      case Tree.Ref(_, _, children) ⇒ children
      case x ⇒ Seq(x)
    }))
  }

  implicit def `Generic as Tree`[A <: AnyRef, L <: HList](
    implicit generic: Generic.Aux[A, L], hListAsTree: Lazy[AsTree[L]]
  ): AsTree[A] = new AsTree[A] {
    def tree(value: A) = hListAsTree.value.tree(generic.to(value)) match {
      case r: Tree.Ref ⇒ Tree.Ref(value, r.children)
      case x ⇒ x
    }
  }
}
