package reftree.core

import shapeless._
import shapeless.labelled.FieldType

/**
 * Generic derivation for [[ToRefTree]]
 *
 * This works for case class-like types, where for each field an instance of [[ToRefTree]]
 * exists or can be derived using this same facility.
 */
trait GenericInstances {
  /** A class that allows to configure how fields of a data structure of type [[A]] are handled */
  case class FieldConfig[A](
    renames: Map[String, Option[String]] = Map.empty[String, Option[String]],
    highlights: Map[String, A ⇒ Boolean] = Map.empty[String, A ⇒ Boolean],
    elides: Map[String, A ⇒ Boolean] = Map.empty[String, A ⇒ Boolean]
  ) {
    /** Rename a field */
    def rename(field: String, name: String) =
      copy[A](renames = renames + (field → Some(name)))

    /** Display the field without any name */
    def noName(field: String) =
      copy[A](renames = renames + (field → None))

    /** Highlight a field */
    def highlight(field: String) =
      copy[A](highlights = highlights + (field → (_ ⇒ true)))

    /** Highlight a field if the data structure satisfies a condition */
    def highlight(field: String, condition: A ⇒ Boolean) =
      copy[A](highlights = highlights + (field → condition))

    /** Elide a field */
    def elide(field: String) =
      copy[A](elides = elides + (field → (_ ⇒ true)))

    /** Elide a field if the data structure satisfies a condition */
    def elide(field: String, condition: A ⇒ Boolean) =
      copy[A](elides = elides + (field → condition))
  }

  object FieldConfig {
    /** Build a [[FieldConfig]] for type [[A]] */
    def apply[A]: FieldConfig[A] = FieldConfig[A]()
  }

  /** We use a different typeclass to avoid defining nonsense ToRefTree instances */
  trait GenericToRefTree[A, R] {
    def refTree(value: A, repr: R): RefTree
  }

  object GenericToRefTree {
    def apply[A, R](toRefTree: (A, R) ⇒ RefTree) = new GenericToRefTree[A, R] {
      def refTree(value: A, repr: R) = toRefTree(value, repr)
    }
  }

  implicit def `Generic HNil RefTree`[A]: GenericToRefTree[A, HNil] =
    GenericToRefTree[A, HNil]((_, r) ⇒ RefTree.Ref(r, Seq.empty))

  implicit def `Generic HCons RefTree`[A, K <: Symbol, H, T <: HList](
    implicit witness: Witness.Aux[K],
    headAsTree: Lazy[ToRefTree[H]],
    tailAsTree: GenericToRefTree[A, T],
    fieldConfig: FieldConfig[A] = FieldConfig[A]()
  ): GenericToRefTree[A, FieldType[K, H] :: T] =
    GenericToRefTree[A, FieldType[K, H] :: T] { (value, repr) ⇒
      val fieldName = witness.value.name
      val head = headAsTree.value.refTree(repr.head: H)
        .withHighlight(fieldConfig.highlights.get(fieldName).exists(_(value)))
        .withElide(fieldConfig.elides.get(fieldName).exists(_(value)))
        .toField.copy(name = fieldConfig.renames.getOrElse(fieldName, Some(fieldName)))
      val tail = tailAsTree.refTree(value, repr.tail) match {
        case RefTree.Ref(_, _, children, _, _) ⇒ children
        case x ⇒ Seq(x.toField)
      }
      RefTree.Ref(repr, head +: tail)
    }

  implicit def `Generic CNil RefTree`[A]: GenericToRefTree[A, CNil] =
    GenericToRefTree[A, CNil]((_, r) ⇒ RefTree.Ref(r, Seq.empty))

  implicit def `Generic CCons RefTree`[A, K <: Symbol, H, T <: Coproduct](
    implicit headAsTree: Lazy[ToRefTree[H]],
    tailAsTree: GenericToRefTree[A, T]
  ): GenericToRefTree[A, FieldType[K, H] :+: T] =
    GenericToRefTree[A, FieldType[K, H] :+: T] {
      case (_, Inl(head)) ⇒ headAsTree.value.refTree(head: H)
      case (value, Inr(tail)) ⇒ tailAsTree.refTree(value, tail)
    }

  implicit def `Generic RefTree`[A <: AnyRef, R](
    implicit generic: LabelledGeneric.Aux[A, R], genericAsTree: Lazy[GenericToRefTree[A, R]]
  ): ToRefTree[A] = ToRefTree[A] { value ⇒
    genericAsTree.value.refTree(value, generic.to(value)) match {
      case r: RefTree.Ref ⇒ RefTree.Ref(value, r.children)
      case x ⇒ x
    }
  }
}
