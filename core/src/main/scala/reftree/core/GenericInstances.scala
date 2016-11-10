package reftree.core

import shapeless._

/**
 * Generic derivation for [[ToRefTree]]
 *
 * This works for case class-like types, where for each field an instance of [[ToRefTree]]
 * exists or can be derived using this same facility.
 */
trait GenericInstances {
  /** We use a different typeclass to avoid defining nonsense ToRefTree instances */
  trait GenericToRefTree[A] {
    def refTree(value: A): RefTree
  }

  object GenericToRefTree {
    def apply[A](toRefTree: A ⇒ RefTree) = new GenericToRefTree[A] {
      def refTree(value: A) = toRefTree(value)
    }
  }

  implicit val `Generic HNil RefTree`: GenericToRefTree[HNil] =
    GenericToRefTree[HNil](RefTree.Ref(_, Seq.empty))

  implicit def `Generic HCons RefTree`[H, T <: HList](
    implicit headAsTree: Lazy[ToRefTree[H]], tailAsTree: GenericToRefTree[T]
  ): GenericToRefTree[H :: T] =
    GenericToRefTree[H :: T] { value ⇒
      val head = headAsTree.value.refTree(value.head)
      val tail = tailAsTree.refTree(value.tail) match {
        case RefTree.Ref(_, _, children, _, _) ⇒ children
        case x ⇒ Seq(x)
      }
      RefTree.Ref(value, head +: tail)
    }

  implicit val `Generic CNil RefTree`: GenericToRefTree[CNil] =
    GenericToRefTree[CNil](RefTree.Ref(_, Seq.empty))

  implicit def `Generic CCons RefTree`[H, T <: Coproduct](
    implicit headAsTree: Lazy[ToRefTree[H]], tailAsTree: GenericToRefTree[T]
  ): GenericToRefTree[H :+: T] =
    GenericToRefTree[H :+: T] {
      case Inl(head) ⇒ headAsTree.value.refTree(head)
      case Inr(tail) ⇒ tailAsTree.refTree(tail)
    }

  implicit def `Generic RefTree`[A <: AnyRef, R](
    implicit generic: Generic.Aux[A, R], genericAsTree: Lazy[GenericToRefTree[R]]
  ): ToRefTree[A] = ToRefTree[A] { value ⇒
    genericAsTree.value.refTree(generic.to(value)) match {
      case r: RefTree.Ref ⇒ RefTree.Ref(value, r.children)
      case x ⇒ x
    }
  }
}
