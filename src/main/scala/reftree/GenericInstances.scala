package reftree

import shapeless._

trait GenericInstances {
  implicit val `HNil RefTree`: ToRefTree[HNil] = new ToRefTree[HNil] {
    def refTree(value: HNil) = RefTree.Ref(value, Seq.empty)
  }

  implicit def `HCons RefTree`[H: ToRefTree, T <: HList: ToRefTree]: ToRefTree[H :: T] = new ToRefTree[H :: T] {
    def refTree(value: H :: T): RefTree = RefTree.Ref(value, value.head.refTree +: (value.tail.refTree match {
      case RefTree.Ref(_, _, children) ⇒ children
      case x ⇒ Seq(x)
    }))
  }

  implicit def `Generic RefTree`[A <: AnyRef, L <: HList](
    implicit generic: Generic.Aux[A, L], hListAsTree: Lazy[ToRefTree[L]]
  ): ToRefTree[A] = new ToRefTree[A] {
    def refTree(value: A) = hListAsTree.value.refTree(generic.to(value)) match {
      case r: RefTree.Ref ⇒ RefTree.Ref(value, r.children)
      case x ⇒ x
    }
  }
}
