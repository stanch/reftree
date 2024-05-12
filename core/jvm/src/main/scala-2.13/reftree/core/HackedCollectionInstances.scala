package scala.collection.immutable

import reftree.core._
import reftree.util.Reflection.PrivateFields

/**
 * [[ToRefTree]] instances for Scala immutable collections, which require access to private fields
 *
 * The package name is intentionally changed so that we can get access to some private fields and classes.
 */
trait HackedCollectionInstances extends CollectionInstances {
  implicit def `Queue RefTree`[A: ToRefTree](implicit list: ToRefTree[List[A]]): ToRefTree[Queue[A]] =
    ToRefTree[Queue[A]] { value =>
      val front = value.privateField[List[A]]("out").refTree.toField.withName("front")
      val back = value.privateField[List[A]]("in").refTree.toField.withName("back")
      RefTree.Ref(value, Seq(front, back))
    }
}
