package reftree.contrib

import reftree._
import zipper.Zipper

object SimplifiedInstances {
  implicit def option[A: ToRefTree]: ToRefTree[Option[A]] = ToRefTree[Option[A]] {
    case None ⇒ RefTree.Null()
    case Some(value) ⇒ value.refTree
  }

  implicit def list[A: ToRefTree]: ToRefTree[List[A]] = ToRefTree[List[A]] {
    case Nil ⇒ RefTree.Null()
    case value ⇒ RefTree.Ref(value, value.map(_.refTree)).copy(name = "List")
  }

  implicit def zipper[A](implicit default: ToRefTree[Zipper[A]]): ToRefTree[Zipper[A]] =
    default.suppressField(3)
}
