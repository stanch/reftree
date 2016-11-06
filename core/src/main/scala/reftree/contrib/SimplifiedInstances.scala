package reftree.contrib

import reftree.core._
import zipper.Zipper

/**
 * This object includes several simplified [[RefTree]] representations
 * that can be used for the benefit of visualization:
 *  - `option` “unboxes” [[Option]], representing [[None]] with `null` and [[Some]] with its inner value;
 *  - `list` and `seq` hide implementation details from [[List]]s and [[Seq]]s respectively,
 *     as if they were simple [[Array]]s;
 *  - `map` represents a “fictional” high-level map with no internal structure;
 *  - `zipper` elides the parent zipper field from a [[Zipper]] visualization.
 */
object SimplifiedInstances {
  /** An “unboxed” [[Option]], representing [[None]] with `null` and [[Some]] with its inner value */
  implicit def option[A: ToRefTree]: ToRefTree[Option[A]] = ToRefTree[Option[A]] {
    case None ⇒ RefTree.Null()
    case Some(value) ⇒ value.refTree
  }

  /** A simplified representation of a [[List]], similar to that of an [[Array]] */
  implicit def list[A: ToRefTree]: ToRefTree[List[A]] = ToRefTree[List[A]] {
    case Nil ⇒ RefTree.Null()
    case value ⇒ RefTree.Ref(value, value.map(_.refTree)).rename("List")
  }

  /** A simplified representation of any [[Seq]], similar to that of an [[Array]] */
  implicit def seq[A: ToRefTree]: ToRefTree[Seq[A]] = ToRefTree[Seq[A]] { value ⇒
    if (value.isEmpty) RefTree.Null() else {
      RefTree.Ref(value, value.map(_.refTree)).rename("Seq")
    }
  }

  /** A simplified representation of a [[Map]], consisting of just key-value pairs */
  implicit def map[A: ToRefTree, B: ToRefTree]: ToRefTree[Map[A, B]] = ToRefTree[Map[A, B]] { value ⇒
    if (value.isEmpty) RefTree.Null() else {
      RefTree.Ref(value, value.toSeq map {
        case tuple @ (k, v) ⇒ RefTree.Ref(tuple, Seq(k.refTree, v.refTree)).rename("MapEntry")
      }).rename("Map")
    }
  }

  /** A simplified representation of a [[Zipper]], eliding its parent */
  implicit def zipper[A](implicit default: ToRefTree[Zipper[A]]): ToRefTree[Zipper[A]] =
    default.highlightField(1).elideField(3)
}
