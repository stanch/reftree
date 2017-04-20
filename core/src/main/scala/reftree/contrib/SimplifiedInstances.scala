package reftree.contrib

import reftree.core._

/**
 * This object includes several simplified [[RefTree]] representations
 * that can be used for the benefit of visualization:
 *  - `option` “unboxes” [[Option]], representing [[None]] with `null` and [[Some]] with its inner value;
 *  - `list` and `seq` hide implementation details from [[List]]s and [[Seq]]s respectively,
 *     as if they were simple [[Array]]s;
 *  - `map` represents a “fictional” high-level map with no internal structure.
 */
object SimplifiedInstances {
  /** A more compact representation of a [[String]], which jams everything into a single box */
  implicit def string: ToRefTree[String] =
    ToRefTree[String] { s ⇒
      val shorter = if (s.length <= 60) s else s"${s.take(30)}...${s.takeRight(30)}"
      RefTree.Ref(s, Seq.empty).rename(s""""$shorter"""")
    }

  /** An “unboxed” [[Option]], representing [[None]] with `null` and [[Some]] with its inner value */
  implicit def option[A: ToRefTree]: ToRefTree[Option[A]] = ToRefTree[Option[A]] {
    case None ⇒ RefTree.Null()
    case Some(value) ⇒ value.refTree
  }

  /** A simplified representation of a [[List]], similar to that of an [[Array]] */
  implicit def list[A: ToRefTree]: ToRefTree[List[A]] = ToRefTree[List[A]] {
    case Nil ⇒ RefTree.Null()
    case value ⇒ RefTree.Ref(value, value.map(_.refTree.toField)).rename("List")
  }

  /** A simplified representation of any [[Seq]], similar to that of an [[Array]] */
  implicit def seq[A: ToRefTree]: ToRefTree[Seq[A]] = ToRefTree[Seq[A]] { value ⇒
    if (value.isEmpty) RefTree.Null() else {
      RefTree.Ref(value, value.map(_.refTree.toField)).rename("Seq")
    }
  }

  /** A simplified representation of a [[Map]], consisting of just key-value pairs */
  implicit def map[A: ToRefTree, B: ToRefTree]: ToRefTree[Map[A, B]] = ToRefTree[Map[A, B]] { value ⇒
    if (value.isEmpty) RefTree.Null() else {
      RefTree.Ref(value, value.toSeq map {
        case tuple @ (k, v) ⇒
          RefTree.Ref(tuple, Seq(k.refTree.toField, v.refTree.toField)).rename("MapEntry").toField
      }).rename("Map")
    }
  }
}
