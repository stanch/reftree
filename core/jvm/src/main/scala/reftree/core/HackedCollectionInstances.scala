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
    ToRefTree[Queue[A]] { value ⇒
      val front = value.privateField[List[A]]("out").refTree.toField.withName("front")
      val back = value.privateField[List[A]]("in").refTree.toField.withName("back")
      RefTree.Ref(value, Seq(front, back))
    }

  private def vectorArrayRefTree[A: ToRefTree](value: Array[AnyRef], depth: Int): RefTree = {
    RefTree.Ref(value, value map { x ⇒
      if (x == null) RefTree.Null()
      else if (depth > 0) vectorArrayRefTree[A](x.asInstanceOf[Array[AnyRef]], depth - 1)
      else x.asInstanceOf[A].refTree
    } map (_.toField)).rename("Array")
  }

  implicit def `Vector RefTree`[A: ToRefTree]: ToRefTree[Vector[A]] = ToRefTree[Vector[A]] { value ⇒
    val start = value.startIndex.refTree.toField.withName("start")
    val end = value.endIndex.refTree.toField.withName("end")
    val focus = RefTree.Val(value.privateField[Int]("focus")).withHint(RefTree.Val.Bin)
      .toField.withName("focus")
    val depth = value.depth.refTree.toField.withName("depth")
    val layers = Seq(
      value.display0, value.display1,
      value.display2, value.display3,
      value.display4, value.display5
    ).zipWithIndex.map {
      case (layer, d) if d < value.depth ⇒ vectorArrayRefTree[A](layer, d)
      case (layer, _) ⇒ RefTree.Null()
    }.map(_.toField)
    RefTree.Ref(
      value,
      Seq(start, end, focus, depth) ++ layers
    )
  }

  implicit def `HashSet RefTree`[A: ToRefTree]: ToRefTree[HashSet[A]] =
    ToRefTree[HashSet[A]] {
      case leaf: HashSet.HashSet1[A] ⇒
        val hash = RefTree.Val(leaf.privateField[Int]("hash")).withHint(RefTree.Val.Hex)
          .toField.withName("hash")
        val key = leaf.privateField[A]("key").refTree.toField
        RefTree.Ref(leaf, Seq(hash, key)).rename("HashSet.HashSet1")
      case collision: HashSet.HashSetCollision1[A] ⇒
        val hash = RefTree.Val(collision.privateField[Int]("hash")).withHint(RefTree.Val.Hex)
          .toField.withName("hash")
        val ks = collision.privateField[ListSet[A]]("ks").refTree.toField
        RefTree.Ref(collision, Seq(hash, ks)).rename("HashSet.HashSetCollision1")
      case trie: HashSet.HashTrieSet[A] ⇒
        val size = trie.privateField[Int]("size0").refTree.toField.withName("size")
        val elems = trie.privateField[Array[HashSet[A]]]("elems").refTree.toField
        val bitmap = RefTree.Val(trie.privateField[Int]("bitmap")).withHint(RefTree.Val.Bin)
          .toField.withName("bitmap")
        RefTree.Ref(trie, Seq(size, bitmap, elems)).rename("HashSet.HashTrieSet")
      case empty ⇒
        RefTree.Ref(empty, Seq.empty).rename("HashSet.EmptyHashSet")
    }

  implicit def `HashMap RefTree`[A: ToRefTree, B: ToRefTree]: ToRefTree[HashMap[A, B]] =
    ToRefTree[HashMap[A, B]] {
      case leaf: HashMap.HashMap1[A, B] ⇒
        val hash = RefTree.Val(leaf.privateField[Int]("hash")).withHint(RefTree.Val.Hex)
          .toField.withName("hash")
        val key = leaf.privateField[A]("key").refTree.toField
        val value = leaf.privateField[A]("value").refTree.toField
        RefTree.Ref(leaf, Seq(hash, key, value)).rename("HashMap.HashMap1")
      case collision: HashMap.HashMapCollision1[A, B] ⇒
        val hash = RefTree.Val(collision.privateField[Int]("hash")).withHint(RefTree.Val.Hex)
          .toField.withName("hash")
        val kvs = collision.privateField[ListMap[A, B]]("kvs").refTree.toField
        RefTree.Ref(collision, Seq(hash, kvs)).rename("HashMap.HashMapCollision1")
      case trie: HashMap.HashTrieMap[A, B] ⇒
        val size = trie.privateField[Int]("size0").refTree.toField.withName("size")
        val elems = trie.privateField[Array[HashMap[A, B]]]("elems").refTree.toField
        val bitmap = RefTree.Val(trie.privateField[Int]("bitmap")).withHint(RefTree.Val.Bin)
          .toField.withName("bitmap")
        RefTree.Ref(trie, Seq(size, bitmap, elems)).rename("HashMap.HashTrieMap")
      case empty ⇒
        RefTree.Ref(empty, Seq.empty).rename("HashMap.EmptyHashMap")
    }

  private def redBlackTreeRefTree[A: ToRefTree, B: ToRefTree](
    tree: RedBlackTree.Tree[A, B],
    includeValue: Boolean
  ): RefTree = {
    if (tree == null) RefTree.Null() else {
      val key = tree.key.refTree.toField
      val value = if (includeValue) Seq(tree.value.refTree.toField) else Seq.empty
      val left = redBlackTreeRefTree(tree.left, includeValue).toField
      val right = redBlackTreeRefTree(tree.right, includeValue).toField
      RefTree.Ref(tree, Seq(key) ++ value ++ Seq(left, right))
        .copy(highlight = tree.isInstanceOf[RedBlackTree.RedTree[A, B]])
    }
  }

  implicit def `TreeSet RefTree`[A: ToRefTree]: ToRefTree[TreeSet[A]] = {
    implicit val unit = ToRefTree[Unit](_ ⇒ RefTree.Null())
    ToRefTree[TreeSet[A]] { value ⇒
      if (value.isEmpty) {
        RefTree.Ref(value, Seq.empty)
      } else {
        val underlying = value.privateField[RedBlackTree.Tree[A, Unit]]("tree")
        val children = redBlackTreeRefTree(underlying, includeValue = false).asInstanceOf[RefTree.Ref].children
        RefTree.Ref(value, children)
      }
    }
  }

  implicit def `TreeMap RefTree`[A: ToRefTree, B: ToRefTree]: ToRefTree[TreeMap[A, B]] =
    ToRefTree[TreeMap[A, B]] { value ⇒
      if (value.isEmpty) {
        RefTree.Ref(value, Seq.empty)
      } else {
        val underlying = value.privateField[RedBlackTree.Tree[A, B]]("tree")
        val children = redBlackTreeRefTree(underlying, includeValue = true).asInstanceOf[RefTree.Ref].children
        RefTree.Ref(value, children)
      }
    }
}
