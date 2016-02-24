package scala.collection.immutable

import diapers.{Tree, AsTree}
import AsTree._

object AsTreeInstances {
  implicit class PrivateFields[A](val value: A) extends AnyVal {
    def privateField[B](name: String) = {
      val field = value.getClass.getDeclaredField(name)
      field.setAccessible(true)
      field.get(value).asInstanceOf[B]
    }
  }

  def hashSet[A: AsTree]: AsTree[HashSet[A]] = new AsTree[HashSet[A]] {
    def tree(value: HashSet[A]): Tree = value match {
      case leaf: HashSet.HashSet1[A] ⇒
        val hash = leaf.privateField[Int]("hash")
        val key = leaf.privateField[A]("key")
        Tree.Ref(leaf, Seq(hash.tree, key.tree)).copy(name = "HashSet.HashSet1")
      case collision: HashSet.HashSetCollision1[A] ⇒
        val hash = collision.privateField[Int]("hash")
        val ks = collision.privateField[ListSet[A]]("ks")
        Tree.Ref(collision, Seq(hash.tree, ks.tree)).copy(name = "HashSet.HashSetCollision1")
      case trie: HashSet.HashTrieSet[A] ⇒
        val size = trie.privateField[Int]("size0")
        val bitmap = trie.privateField[Int]("bitmap")
        val elems = trie.privateField[Array[HashSet[A]]]("elems")
        val hexBitmap = Tree.Val(bitmap, Some(Tree.Val.Hex))
        Tree.Ref(trie, Seq(size.tree, hexBitmap, elems.tree)).copy(name = "HashSet.HashTrieSet")
    }
  }
}
