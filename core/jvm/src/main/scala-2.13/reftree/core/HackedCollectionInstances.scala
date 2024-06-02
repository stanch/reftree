package scala.collection.immutable

import reftree.core._
import Reflection._

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

  private def vectorArrayRefTree[A: ToRefTree](value: Array[_ <: AnyRef], depth: Int): RefTree =
    RefTree.Ref(
      value,
      value.toVector.map { x =>
        if (x == null) RefTree.Null()
        else if (depth > 0) vectorArrayRefTree[A](x.asInstanceOf[Array[AnyRef]], depth - 1)
        else x.asInstanceOf[A].refTree
      }.map(_.toField)
    ).rename("Array")

  implicit def `Vector RefTree`[A: ToRefTree]: ToRefTree[Vector[A]] = ToRefTree[Vector[A]] { value =>
    val start = value.startIndex.refTree.toField.withName("start")
    val end = value.endIndex.refTree.toField.withName("end")
    val sliceCount = value.vectorSliceCount.refTree.toField.withName("sliceCount")

    def traverseVector(vector: Vector[A]): Seq[RefTree.Ref.Field] =
      if (vector.vectorSliceCount > 1) {
        val sliceCount = vector.vectorSliceCount
        val middle = (sliceCount + 1) / 2
        val slices = (0 until sliceCount).map(value.vectorSlice)

        val (beforeMiddle, afterMiddle) = slices.splitAt(middle)

        (beforeMiddle.zipWithIndex ++ afterMiddle.zip((0 until (middle - 1)).reverse)).map {
          case (layer, d) if d < vector.vectorSliceCount =>
            vectorArrayRefTree[A](layer, d)
          case (_, _) => RefTree.Null()
        }.map(_.toField)
      } else {
        val arr =
          if (vector.vectorSliceCount > 0) vector.vectorSlice(0)
          else Array.empty

        vectorArrayRefTree[A](arr, 0).toField :: Nil
      }

    val layers =
      traverseVector(value)

    RefTree.Ref(
      value,
      Seq(start, end, sliceCount) ++ layers
    )
  }

  implicit def `HashSet RefTree`[A: ToRefTree]: ToRefTree[HashSet[A]] = ToRefTree[HashSet[A]] { value =>
    bitMapIndexedSetNodeRefTree[A](value.rootNode).rename("HashSet")
  }

  private def bitMapIndexedSetNodeRefTree[A: ToRefTree](node: BitmapIndexedSetNode[A]): RefTree.Ref = {
    val hash =
      RefTree.Val.formatted(node.cachedJavaKeySetHashCode)(_.toHexString)
        .toField.withName("hash")
    val size = node.size.refTree.toField.withName("size")
    val nodeArity = node.nodeArity.refTree.toField.withName("nodeArity")
    val payloadArity = node.payloadArity.refTree.toField.withName("payloadArity")

    val content =
      RefTree.Ref(
          node.content,
          hashSetArrayRefTree[A](node.content, node.nodeArity)
        )
        .rename("Array")
        .toField

    RefTree.Ref(
      node,
      Seq(hash, size, nodeArity, payloadArity, content)
    )
  }

  private def hashSetArrayRefTree[A: ToRefTree](content: Array[Any], arity: Int): Seq[RefTree.Ref.Field] =
    if (arity > 0) {
      content.toVector.map { x =>
        if (x.isInstanceOf[BitmapIndexedSetNode[_]])
          bitMapIndexedSetNodeRefTree[A](x.asInstanceOf[BitmapIndexedSetNode[A]])
        else
          x.asInstanceOf[A].refTree
      }.map(_.toField)
    } else
      content.toVector.map(_.asInstanceOf[A].refTree.toField)

  implicit def `HashMap RefTree`[A: ToRefTree, B: ToRefTree]: ToRefTree[HashMap[A, B]] =
    ToRefTree[HashMap[A, B]] { value =>
      bitmapIndexedMapNodeRefTree[A, B](value.rootNode).rename("HashMap")
    }

  private def bitmapIndexedMapNodeRefTree[A: ToRefTree, B: ToRefTree](
    node: BitmapIndexedMapNode[A, B]
  ): RefTree.Ref = {
    val hash =
      RefTree.Val.formatted(node.cachedJavaKeySetHashCode)(_.toHexString)
        .toField.withName("hash")
    val size = node.size.refTree.toField.withName("size")
    val nodeArity = node.nodeArity.refTree.toField.withName("nodeArity")
    val payloadArity = node.payloadArity.refTree.toField.withName("payloadArity")

    val content =
      RefTree.Ref(
        node.content,
        hashMapArrayRefTree[A, B](node.content, node.nodeArity)
      )
      .rename("Array")
      .toField

    RefTree.Ref(
      node,
      Seq(hash, size, nodeArity, payloadArity, content)
    )
  }

  private def hashMapArrayRefTree[A: ToRefTree, B: ToRefTree](
    content: Array[Any],
    arity: Int
  ): Seq[RefTree.Ref.Field] =
  if (arity > 0) {
    val mapNodesBuilder = Vector.newBuilder[RefTree.Ref]
    val keysBuilder = Vector.newBuilder[A]
    val valuesBuilder = Vector.newBuilder[B]

    content.toVector.zipWithIndex.foreach {
      case (el, _) if el.isInstanceOf[BitmapIndexedMapNode[_, _]] =>
        mapNodesBuilder += bitmapIndexedMapNodeRefTree[A, B](el.asInstanceOf[BitmapIndexedMapNode[A, B]])
      case (el, i) if i % 2 == 0 =>
        keysBuilder += el.asInstanceOf[A]
      case (el, _) =>
        valuesBuilder += el.asInstanceOf[B]
    }

    (keysBuilder.result().zip(valuesBuilder.result()).map(_.refTree) ++
      mapNodesBuilder.result()).map(_.toField)
  } else {
    val keysBuilder = Vector.newBuilder[A]
    val valuesBuilder = Vector.newBuilder[B]

    content.toVector.zipWithIndex.foreach {
      case (el, i) if i % 2 == 0 =>
        keysBuilder += el.asInstanceOf[A]
      case (el, _) =>
        valuesBuilder += el.asInstanceOf[B]
    }

    keysBuilder.result().zip(valuesBuilder.result()).map(_.refTree.toField)
  }
}

private[immutable] object Reflection {
  /** A utility for accessing private fields */
  implicit class PrivateFields[A](val value: A) extends AnyVal {
    def privateField[B](name: String): B = {
      val field = value.getClass.getDeclaredField(name)
      field.setAccessible(true)
      field.get(value).asInstanceOf[B]
    }
  }
}
