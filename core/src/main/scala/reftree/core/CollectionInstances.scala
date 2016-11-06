package scala.collection.immutable

import reftree.core._
import reftree.util.Reflection.PrivateFields

/**
 * [[ToRefTree]] instances for Scala immutable collections
 *
 * The package name is intentionally changed so that we can get access to some private fields and classes.
 */
trait CollectionInstances {
  implicit def `Option RefTree`[A: ToRefTree]: ToRefTree[Option[A]] = ToRefTree[Option[A]] {
    case value @ Some(a) ⇒ RefTree.Ref(value, Seq(a.refTree))
    case value @ None ⇒ RefTree.Ref(value, Seq.empty).rename("None")
  }

  implicit def `Array RefTree`[A: ToRefTree]: ToRefTree[Array[A]] = ToRefTree[Array[A]] { value ⇒
    RefTree.Ref(value, value.map(_.refTree)).rename("Array")
  }

  implicit def `List RefTree`[A: ToRefTree]: ToRefTree[List[A]] = new ToRefTree[List[A]] {
    def refTree(value: List[A]): RefTree = value match {
      case head :: tail ⇒ RefTree.Ref(value, Seq(head.refTree, refTree(tail))).rename("Cons")
      case Nil ⇒ RefTree.Ref(Nil, Seq.empty).rename("Nil")
    }
  }

  implicit def `Queue RefTree`[A: ToRefTree](implicit list: ToRefTree[List[A]]): ToRefTree[Queue[A]] =
    ToRefTree[Queue[A]] { value ⇒
      val in = value.privateField[List[A]]("in")
      val out = value.privateField[List[A]]("out")
      RefTree.Ref(value, Seq(out.refTree, in.refTree))
    }

  private def vectorArrayRefTree[A: ToRefTree](value: Array[AnyRef], depth: Int): RefTree = {
    RefTree.Ref(value, value map { x ⇒
      if (x == null) RefTree.Null()
      else if (depth > 0) vectorArrayRefTree[A](x.asInstanceOf[Array[AnyRef]], depth - 1)
      else x.asInstanceOf[A].refTree
    }).rename("Array")
  }

  implicit def `Vector RefTree`[A: ToRefTree]: ToRefTree[Vector[A]] = ToRefTree[Vector[A]] { value ⇒
    val focus = value.privateField[Int]("focus")
    val binFocus = RefTree.Val(focus, Some(RefTree.Val.Bin), highlight = false, elide = false)
    val layers = Seq(
      value.display0, value.display1,
      value.display2, value.display3,
      value.display4, value.display5
    ).zipWithIndex.map {
      case (layer, depth) if depth < value.depth ⇒ vectorArrayRefTree[A](layer, depth)
      case (layer, _) ⇒ RefTree.Null()
    }
    RefTree.Ref(
      value,
      Seq(value.startIndex.refTree, value.endIndex.refTree, binFocus, value.depth.refTree) ++ layers
    )
  }

  implicit def `ListSet RefTree`[A: ToRefTree]: ToRefTree[ListSet[A]] =
    new ToRefTree[ListSet[A]] {
      // Technically this is cheating, but there is too much private stuff in ListSet
      // to construct the tree representation by direct introspection.
      // I promise it looks just like the real deal!
      def refTree(value: ListSet[A]): RefTree = value.headOption match {
        case Some(head) ⇒ RefTree.Ref(value, Seq(head.refTree, refTree(value.tail))).rename("ListSet.Node")
        case None ⇒ RefTree.Ref(ListSet.empty[A], Seq()).rename("ListSet.EmptyListSet")
      }
    }

  implicit def `ListMap RefTree`[A: ToRefTree, B: ToRefTree]: ToRefTree[ListMap[A, B]] =
    new ToRefTree[ListMap[A, B]] {
      // Technically this is cheating, but there is too much private stuff in ListMap
      // to construct the tree representation by direct introspection.
      // I promise it looks just like the real deal!
      def refTree(value: ListMap[A, B]): RefTree = value.headOption match {
        case Some((k, v)) ⇒ RefTree.Ref(value, Seq(k.refTree, v.refTree, refTree(value.tail))).rename("ListMap.Node")
        case None ⇒ RefTree.Ref(ListMap.empty[A, B], Seq()).rename("ListMap.EmptyListMap")
      }
    }

  implicit def `HashSet RefTree`[A: ToRefTree]: ToRefTree[HashSet[A]] =
    ToRefTree[HashSet[A]] { value ⇒
      if (value.isEmpty) {
        RefTree.Ref(value, Seq.empty).rename("HashSet.EmptyHashSet")
      } else value match {
        case leaf: HashSet.HashSet1[A] ⇒
          val hash = leaf.privateField[Int]("hash")
          val key = leaf.privateField[A]("key")
          RefTree.Ref(leaf, Seq(hash.refTree, key.refTree)).rename("HashSet.HashSet1")
        case collision: HashSet.HashSetCollision1[A] ⇒
          val hash = collision.privateField[Int]("hash")
          val ks = collision.privateField[ListSet[A]]("ks")
          RefTree.Ref(collision, Seq(hash.refTree, ks.refTree)).rename("HashSet.HashSetCollision1")
        case trie: HashSet.HashTrieSet[A] ⇒
          val size = trie.privateField[Int]("size0")
          val bitmap = trie.privateField[Int]("bitmap")
          val elems = trie.privateField[Array[HashSet[A]]]("elems")
          val binBitmap = RefTree.Val(bitmap, Some(RefTree.Val.Bin), highlight = false, elide = false)
          RefTree.Ref(trie, Seq(size.refTree, binBitmap, elems.refTree)).rename("HashSet.HashTrieSet")
      }
    }

  implicit def `HashMap RefTree`[A: ToRefTree, B: ToRefTree]: ToRefTree[HashMap[A, B]] =
    ToRefTree[HashMap[A, B]] { value ⇒
      if (value.isEmpty) {
        RefTree.Ref(value, Seq.empty).rename("HashMap.EmptyHashMap")
      } else value match {
        case leaf: HashMap.HashMap1[A, B] ⇒
          val hash = leaf.privateField[Int]("hash")
          val key = leaf.privateField[A]("key")
          val value = leaf.privateField[A]("value")
          RefTree.Ref(leaf, Seq(hash.refTree, key.refTree, value.refTree)).rename("HashMap.HashMap1")
        case collision: HashMap.HashMapCollision1[A, B] ⇒
          val hash = collision.privateField[Int]("hash")
          val kvs = collision.privateField[ListMap[A, B]]("kvs")
          RefTree.Ref(collision, Seq(hash.refTree, kvs.refTree)).rename("HashMap.HashMapCollision1")
        case trie: HashMap.HashTrieMap[A, B] ⇒
          val size = trie.privateField[Int]("size0")
          val bitmap = trie.privateField[Int]("bitmap")
          val elems = trie.privateField[Array[HashMap[A, B]]]("elems")
          val binBitmap = RefTree.Val(bitmap, Some(RefTree.Val.Bin), highlight = false, elide = false)
          RefTree.Ref(trie, Seq(size.refTree, binBitmap, elems.refTree)).rename("HashMap.HashTrieMap")
      }
    }

  private def redBlackTreeRefTree[A: ToRefTree, B: ToRefTree](
    tree: RedBlackTree.Tree[A, B],
    includeValue: Boolean
  ): RefTree = {
    if (tree == null) RefTree.Null() else {
      val left = redBlackTreeRefTree(tree.left, includeValue)
      val right = redBlackTreeRefTree(tree.right, includeValue)
      val value = if (includeValue) Seq(tree.value.refTree) else Seq.empty
      RefTree.Ref(tree, Seq(tree.key.refTree) ++ value ++ Seq(left, right))
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
