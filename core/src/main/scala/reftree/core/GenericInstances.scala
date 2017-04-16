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
  /** A class that allows to configure how the derivation of [[ToRefTree]] for [[A]] is done */
  case class DerivationConfig[A](
    name: Option[A ⇒ String] = None,
    omittedFields: Set[String] = Set.empty[String],
    tweakedFields: Map[String, A ⇒ DerivationConfig.FieldUpdate] =
      Map.empty[String, A ⇒ DerivationConfig.FieldUpdate]
  ) {
    /** Rename the tree derived for the data structure */
    def rename(name: String): DerivationConfig[A] =
      renameWith(_ ⇒ name)

    /** Rename the tree derived for the data structure, based on the value being visualized */
    def renameWith(name: A ⇒ String): DerivationConfig[A] =
      copy(name = Some(name))

    /** Omit the field with a given name */
    def omitField(field: String) =
      copy[A](omittedFields = omittedFields + field)

    /** Adjust the field with a given name */
    def tweakField(field: String, update: DerivationConfig.FieldUpdate): DerivationConfig[A] =
      tweakFieldWith(field, Function.const(update))

    /** Adjust the field with a given name, based on the value being visualized */
    def tweakFieldWith(field: String, update: A ⇒ DerivationConfig.FieldUpdate): DerivationConfig[A] =
      copy(tweakedFields = tweakedFields + (field → update))
  }

  object DerivationConfig {
    type FieldUpdate = RefTree.Ref.Field ⇒ RefTree.Ref.Field

    /** Build a [[DerivationConfig]] for type [[A]] */
    def apply[A]: DerivationConfig[A] = DerivationConfig[A]()
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
    derivationConfig: DerivationConfig[A] = DerivationConfig[A]()
  ): GenericToRefTree[A, FieldType[K, H] :: T] =
    GenericToRefTree[A, FieldType[K, H] :: T] { (value, repr) ⇒
      val fieldName = witness.value.name
      val field = headAsTree.value.refTree(repr.head: H).toField.withName(fieldName)
      val head = if (derivationConfig.omittedFields(fieldName)) None else {
        Some(derivationConfig.tweakedFields.get(fieldName).fold(field)(_(value)(field)))
      }
      val tail = tailAsTree.refTree(value, repr.tail) match {
        case RefTree.Ref(_, _, children, _) ⇒ children
        case x ⇒ Seq(x.toField)
      }
      RefTree.Ref(repr, head.toSeq ++ tail)
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
    implicit generic: LabelledGeneric.Aux[A, R],
    genericAsTree: Lazy[GenericToRefTree[A, R]],
    derivationConfig: DerivationConfig[A] = DerivationConfig[A]()
  ): ToRefTree[A] = ToRefTree[A] { value ⇒
    genericAsTree.value.refTree(value, generic.to(value)) match {
      case r: RefTree.Ref ⇒
        val tree = RefTree.Ref(value, r.children)
        derivationConfig.name.fold(tree)(f ⇒ tree.rename(f(value)))
      case x ⇒ x
    }
  }
}
