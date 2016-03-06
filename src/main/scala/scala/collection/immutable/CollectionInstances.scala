package scala.collection.immutable

import diapers.{RefTree, ToRefTree}
import ToRefTree.Syntax

trait CollectionInstances {
  implicit class PrivateFields[A](value: A) {
    def privateField[B](name: String) = {
      val field = value.getClass.getDeclaredField(name)
      field.setAccessible(true)
      field.get(value).asInstanceOf[B]
    }
  }

  implicit def `Array RefTree`[A: ToRefTree]: ToRefTree[Array[A]] = new ToRefTree[Array[A]] {
    def refTree(value: Array[A]): RefTree = RefTree.Ref(value, value.map(_.refTree)).copy(name = "Array")
  }

  implicit def `List RefTree`[A: ToRefTree]: ToRefTree[List[A]] = new ToRefTree[List[A]] {
    def refTree(value: List[A]): RefTree = value match {
      case head :: tail ⇒ RefTree.Ref(value, Seq(head.refTree, refTree(tail))).copy(name = "Cons")
      case Nil ⇒ RefTree.Ref(Nil, Seq.empty).copy(name = "Nil")
    }
  }

  implicit def `Queue RefTree`[A: ToRefTree]: ToRefTree[Queue[A]] = new ToRefTree[Queue[A]] {
    def refTree(value: Queue[A]): RefTree = {
      val in = value.privateField[List[A]]("in")
      val out = value.privateField[List[A]]("out")
      RefTree.Ref(value, Seq(in.refTree, out.refTree))
    }
  }

  implicit def `ListSet RefTree`[A: ToRefTree]: ToRefTree[ListSet[A]] = new ToRefTree[ListSet[A]] {
    // Technically this is cheating, but there is too much private stuff in ListSet
    // to construct the tree representation by direct introspection.
    // I promise it looks just like the real deal!
    def refTree(value: ListSet[A]): RefTree = value.headOption match {
      case Some(head) ⇒ RefTree.Ref(value, Seq(head.refTree, refTree(value.tail))).copy(name = "ListSet.Node")
      case None ⇒ RefTree.Ref(ListSet.empty[A], Seq()).copy(name = "ListSet.EmptyListSet")
    }
  }

  implicit def `HashSet RefTree`[A: ToRefTree]: ToRefTree[HashSet[A]] = new ToRefTree[HashSet[A]] {
    def refTree(value: HashSet[A]): RefTree = value match {
      case leaf: HashSet.HashSet1[A] ⇒
        val hash = leaf.privateField[Int]("hash")
        val key = leaf.privateField[A]("key")
        RefTree.Ref(leaf, Seq(hash.refTree, key.refTree)).copy(name = "HashSet.HashSet1")
      case collision: HashSet.HashSetCollision1[A] ⇒
        val hash = collision.privateField[Int]("hash")
        val ks = collision.privateField[ListSet[A]]("ks")
        RefTree.Ref(collision, Seq(hash.refTree, ks.refTree)).copy(name = "HashSet.HashSetCollision1")
      case trie: HashSet.HashTrieSet[A] ⇒
        val size = trie.privateField[Int]("size0")
        val bitmap = trie.privateField[Int]("bitmap")
        val elems = trie.privateField[Array[HashSet[A]]]("elems")
        val binBitmap = RefTree.Val(bitmap, Some(RefTree.Val.Bin))
        RefTree.Ref(trie, Seq(size.refTree, binBitmap, elems.refTree)).copy(name = "HashSet.HashTrieSet")
    }
  }
}
