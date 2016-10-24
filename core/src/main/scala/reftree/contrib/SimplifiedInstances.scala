package reftree.contrib

import reftree.core._
import zipper.Zipper

object SimplifiedInstances {
  implicit def option[A: ToRefTree]: ToRefTree[Option[A]] = ToRefTree[Option[A]] {
    case None ⇒ RefTree.Null()
    case Some(value) ⇒ value.refTree
  }

  implicit def list[A: ToRefTree]: ToRefTree[List[A]] = ToRefTree[List[A]] {
    case Nil ⇒ RefTree.Null()
    case value ⇒ RefTree.Ref(value, value.map(_.refTree)).rename("List")
  }

  implicit def seq[A: ToRefTree]: ToRefTree[Seq[A]] = ToRefTree[Seq[A]] { value ⇒
    if (value.isEmpty) RefTree.Null() else {
      RefTree.Ref(value, value.map(_.refTree)).rename("Seq")
    }
  }

  implicit def map[A: ToRefTree, B: ToRefTree]: ToRefTree[Map[A, B]] = ToRefTree[Map[A, B]] { value ⇒
    if (value.isEmpty) RefTree.Null() else {
      RefTree.Ref(value, value.toSeq map {
        case tuple @ (k, v) ⇒ RefTree.Ref(tuple, Seq(k.refTree, v.refTree)).rename("MapEntry")
      }).rename("Map")
    }
  }

  implicit def zipper[A](implicit default: ToRefTree[Zipper[A]]): ToRefTree[Zipper[A]] =
    default.suppressField(3)
}
