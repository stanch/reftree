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
