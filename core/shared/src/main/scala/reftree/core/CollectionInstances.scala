package scala.collection.immutable

import reftree.core._

/**
 * [[ToRefTree]] instances for Scala immutable collections
 *
 * The package name is intentionally changed so that we can get access to some private fields and classes.
 */
trait CollectionInstances {
  implicit def `Option RefTree`[A: ToRefTree]: ToRefTree[Option[A]] = ToRefTree[Option[A]] {
    case value @ Some(a) ⇒ RefTree.Ref(value, Seq(a.refTree.toField))
    case value ⇒ RefTree.Ref(value, Seq.empty).rename("None")
  }

  implicit def `Array RefTree`[A: ToRefTree]: ToRefTree[Array[A]] = ToRefTree[Array[A]] { value ⇒
    RefTree.Ref(value, value.map(_.refTree.toField)).rename("Array")
  }

  implicit def `List RefTree`[A: ToRefTree]: ToRefTree[List[A]] = new ToRefTree[List[A]] {
    def refTree(value: List[A]): RefTree = value match {
      case head :: tail ⇒
        RefTree.Ref(value, Seq(head.refTree.toField, refTree(tail).toField)).rename("Cons")
      case Nil ⇒ RefTree.Ref(Nil, Seq.empty).rename("Nil")
    }
  }

  implicit def `ListSet RefTree`[A: ToRefTree]: ToRefTree[ListSet[A]] =
    new ToRefTree[ListSet[A]] {
      // Technically this is cheating, but there is too much private stuff in ListSet
      // to construct the tree representation by direct introspection.
      // I promise it looks just like the real deal!
      def refTree(value: ListSet[A]): RefTree = value.headOption match {
        case Some(head) ⇒
          RefTree.Ref(value, Seq(head.refTree.toField, refTree(value.tail).toField))
            .rename("ListSet.Node")
        case None ⇒ RefTree.Ref(ListSet.empty[A], Seq.empty).rename("ListSet.EmptyListSet")
      }
    }

  implicit def `ListMap RefTree`[A: ToRefTree, B: ToRefTree]: ToRefTree[ListMap[A, B]] =
    new ToRefTree[ListMap[A, B]] {
      // Technically this is cheating, but there is too much private stuff in ListMap
      // to construct the tree representation by direct introspection.
      // I promise it looks just like the real deal!
      def refTree(value: ListMap[A, B]): RefTree = value.headOption match {
        case Some((k, v)) ⇒
          RefTree.Ref(value, Seq(k.refTree.toField, v.refTree.toField, refTree(value.tail).toField))
            .rename("ListMap.Node")
        case None ⇒ RefTree.Ref(ListMap.empty[A, B], Seq.empty).rename("ListMap.EmptyListMap")
      }
    }
}
