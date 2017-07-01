package reftree.contrib

import reftree.core._
import shapeless._
import shapeless.labelled.FieldType

object ShapelessInstances {
  implicit val `HNil RefTree`: ToRefTree[HNil] =
    ToRefTree(_ ⇒ RefTree.Ref(HNil, Seq.empty).rename("HNil"))

  implicit def `HCons RefTree`[H: ToRefTree, T <: HList: ToRefTree]: ToRefTree[H :: T] =
    ToRefTree(list ⇒ RefTree.Ref(list, Seq(list.head.refTree.toField, list.tail.refTree.toField)).rename("HCons"))

  implicit def `KeyTag RefTree`[A: ToRefTree, K <: Symbol]: ToRefTree[A with FieldType[K, A]] =
    ToRefTree { (_: A).refTree }
}
