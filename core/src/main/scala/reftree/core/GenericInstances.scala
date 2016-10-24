package reftree.core

import shapeless._

trait GenericInstances {
  implicit val `HNil RefTree`: ToRefTree[HNil] =
    ToRefTree[HNil](RefTree.Ref(_, Seq.empty))

  implicit def `HCons RefTree`[H: ToRefTree, T <: HList: ToRefTree]: ToRefTree[H :: T] =
    ToRefTree[H :: T] { value ⇒
      RefTree.Ref(value, value.head.refTree +: (value.tail.refTree match {
        case RefTree.Ref(_, _, children, _) ⇒ children
        case x ⇒ Seq(x)
      }))
    }

  implicit def `Generic RefTree`[A <: AnyRef, L <: HList](
    implicit generic: Generic.Aux[A, L], hListAsTree: Lazy[ToRefTree[L]]
  ): ToRefTree[A] = ToRefTree[A] { value ⇒
    hListAsTree.value.refTree(generic.to(value)) match {
      case r: RefTree.Ref ⇒ RefTree.Ref(value, r.children)
      case x ⇒ x
    }
  }
}
