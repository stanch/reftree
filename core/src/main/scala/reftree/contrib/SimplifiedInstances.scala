package reftree.contrib

import reftree._
import zipper.Zipper

object SimplifiedInstances {
  implicit def option[A: ToRefTree]: ToRefTree[Option[A]] = new ToRefTree[Option[A]] {
    def refTree(value: Option[A]): RefTree = value match {
      case None ⇒ RefTree.Null()
      case Some(v) ⇒ v.refTree
    }
  }

  implicit def list[A: ToRefTree]: ToRefTree[List[A]] = new ToRefTree[List[A]] {
    def refTree(value: List[A]): RefTree = value match {
      case Nil ⇒ RefTree.Null()
      case _ ⇒ RefTree.Ref(value, value.map(_.refTree)).copy(name = "List")
    }
  }

  implicit def zipper[A](implicit default: ToRefTree[Zipper[A]]): ToRefTree[Zipper[A]] =
    default.suppressField(3)
}
