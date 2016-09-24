package scala.collection.immutable

import reftree.{RefTree, ToRefTree, RefTreeSyntax}

trait CollectionInstances {
  implicit class PrivateFields[A](value: A) {
    def privateField[B](name: String) = {
      val field = value.getClass.getDeclaredField(name)
      field.setAccessible(true)
      field.get(value).asInstanceOf[B]
    }
  }

  implicit def `Option RefTree`[A: ToRefTree]: ToRefTree[Option[A]] = new ToRefTree[Option[A]] {
    def refTree(value: Option[A]) = value match {
      case Some(a) ⇒ RefTree.Ref(value, Seq(a.refTree))
      case None ⇒ RefTree.Ref(value, Seq.empty).copy(name = "None")
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

  implicit def `Queue RefTree`[A: ToRefTree](implicit list: ToRefTree[List[A]]): ToRefTree[Queue[A]] = new ToRefTree[Queue[A]] {
    def refTree(value: Queue[A]): RefTree = {
      val in = value.privateField[List[A]]("in")
      val out = value.privateField[List[A]]("out")
      RefTree.Ref(value, Seq(out.refTree, in.refTree))
    }
  }

  private def vectorArrayRefTree[A: ToRefTree](value: Array[AnyRef], depth: Int): RefTree = {
    RefTree.Ref(value, value map { x ⇒
      if (x == null) RefTree.Null()
      else if (depth > 0) vectorArrayRefTree[A](x.asInstanceOf[Array[AnyRef]], depth - 1)
      else x.asInstanceOf[A].refTree
    }).copy(name = "Array")
  }

  implicit def `Vector RefTree`[A: ToRefTree]: ToRefTree[Vector[A]] = new ToRefTree[Vector[A]] {
    def refTree(value: Vector[A]): RefTree = {
      val focus = value.privateField[Int]("focus")
      val binFocus = RefTree.Val(focus, Some(RefTree.Val.Bin), highlight = false)
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
    def refTree(value: HashSet[A]): RefTree = {
      if (value.isEmpty) {
        RefTree.Ref(value, Seq.empty).copy(name = "HashSet.EmptyHashSet")
      } else value match {
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
          val binBitmap = RefTree.Val(bitmap, Some(RefTree.Val.Bin), highlight = false)
          RefTree.Ref(trie, Seq(size.refTree, binBitmap, elems.refTree)).copy(name = "HashSet.HashTrieSet")
      }
    }
  }

  implicit def `RedBlackTree RefTree`[A: ToRefTree]: ToRefTree[RedBlackTree.Tree[A, Unit]] =
    new ToRefTree[RedBlackTree.Tree[A, Unit]] {
      def refTree(value: RedBlackTree.Tree[A, Unit]): RefTree = {
        if (value == null) RefTree.Null() else {
          RefTree.Ref(value, Seq(value.key.refTree, value.left.refTree, value.right.refTree))
            .copy(highlight = value.isInstanceOf[RedBlackTree.RedTree[A, Unit]])
        }
      }
    }

  implicit def `TreeSet RefTree`[A: ToRefTree]: ToRefTree[TreeSet[A]] = new ToRefTree[TreeSet[A]] {
    def refTree(value: TreeSet[A]): RefTree = {
      if (value.isEmpty) {
        RefTree.Ref(value, Seq.empty)
      } else {
        val underlying = value.privateField[RedBlackTree.Tree[A, Unit]]("tree")
        RefTree.Ref(value, underlying.refTree.asInstanceOf[RefTree.Ref].children)
      }
    }
  }

  object Actual {
    implicit def option[A: ToRefTree]: ToRefTree[Option[A]] = `Option RefTree`[A]
    implicit def list[A: ToRefTree]: ToRefTree[List[A]] = `List RefTree`[A]
  }

  object Simple {
    implicit def option[A: ToRefTree]: ToRefTree[Option[A]] = new ToRefTree[Option[A]] {
      def refTree(value: Option[A]): RefTree = value match {
        case None ⇒ RefTree.Null()
        case Some(v) ⇒ v.refTree
      }
    }
    implicit def list[A: ToRefTree]: ToRefTree[List[A]] = new ToRefTree[List[A]] {
      def refTree(value: List[A]): RefTree = value match {
        case Nil ⇒ RefTree.Null()
        case _ ⇒ RefTree.Ref(value, value.map(_.refTree)).copy(name = "List")
      }
    }
  }
}
