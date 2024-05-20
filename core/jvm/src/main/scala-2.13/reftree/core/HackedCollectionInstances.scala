package scala.collection.immutable

import reftree.core._
import reftree.util.Reflection.PrivateFields

import scala.util.Try

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
      value.map { x =>
        if (x == null) RefTree.Null()
        else if (depth > 0) vectorArrayRefTree[A](x.asInstanceOf[Array[AnyRef]], depth - 1)
        else x.asInstanceOf[A].refTree
      }.map(_.toField).toIndexedSeq
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
}
