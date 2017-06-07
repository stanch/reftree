package reftree.contrib

import de.sciss.fingertree.{Measure, FingerTree}
import reftree.core._
import reftree.util.Reflection.PrivateFields

/**
 * [[ToRefTree]] instances for a [[FingerTree]]
 */
object FingerTreeInstances {
  private def measureField[V: ToRefTree, A](node: AnyRef)(implicit measure: Measure[A, V]) = {
    val name = measure match {
      case Measure.Indexed ⇒ Some("size")
      case Measure.SummedIntInt | Measure.SummedIntLong ⇒ Some("sum")
      case Measure.IndexedSummedIntLong ⇒ Some("(size, sum)")
      case Measure.Unit ⇒ None
      case _ ⇒ Some("M")
    }
    name.map(node.privateField[V]("measure").refTree.toField.withName)
  }

  private def fingerTreeRefTree[V: ToRefTree, A: ToRefTree](tree: AnyRef, depth: Int)(
    implicit measure: Measure[A, V]
  ): RefTree = {
    val m = measureField[V, A](tree).toSeq
    tree.getClass.getSimpleName match {
      case "Empty" ⇒
        RefTree.Ref(tree, m).rename("FingerTree.Empty")
      case "Single" ⇒
        val a = digitRefTree[V, A](tree.privateField[AnyRef]("a"), depth - 1).toField
        RefTree.Ref(tree, m :+ a).rename("FingerTree.Single")
      case "Deep" ⇒
        val prefix = digitRefTree[V, A](tree.privateField[AnyRef]("prefix"), depth).toField
        val subtree = fingerTreeRefTree[V, A](tree.privateField[AnyRef]("tree"), depth + 1).toField
        val suffix = digitRefTree[V, A](tree.privateField[AnyRef]("suffix"), depth).toField
        RefTree.Ref(tree, m ++ Seq(prefix, subtree, suffix)).rename("FingerTree.Deep")
    }
  }

  private def digitRefTree[V: ToRefTree, A: ToRefTree](digit: AnyRef, depth: Int)(
    implicit measure: Measure[A, V]
  ): RefTree = {
    if (depth == 0) {
      // this is just A
      digit.asInstanceOf[A].refTree
    } else {
      // this is Digit[V, Digit[V, ... Digit[V, A]]]
      val m = measureField[V, A](digit).toSeq
      val childCount = Map(
        "One" → 1, "Two" → 2,
        "Three" → 3, "Four" → 4
      )(digit.getClass.getSimpleName)
      val children = Seq.tabulate(childCount) { i ⇒
        val child = digit.privateField[AnyRef](s"a${i + 1}")
        digitRefTree[V, A](child, depth - 1).toField
      }
      RefTree.Ref(digit, m ++ children).rename(s"FingerTree.${digit.getClass.getSimpleName}")
    }
  }

  implicit def `FingerTree RefTree`[V: ToRefTree, A: ToRefTree](
    implicit measure: Measure[A, V]
  ): ToRefTree[FingerTree[V, A]] =
    ToRefTree[FingerTree[V, A]](fingerTreeRefTree[V, A](_, depth = 1))
}
