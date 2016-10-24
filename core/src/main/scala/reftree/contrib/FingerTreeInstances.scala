package reftree.contrib

import de.sciss.fingertree.FingerTree
import reftree.core._
import reftree.util.Reflection.PrivateFields

object FingerTreeInstances {
  private def fingerTreeRefTree[V: ToRefTree, A: ToRefTree](tree: AnyRef, depth: Int): RefTree = {
    val measure = tree.privateField[V]("measure").refTree
    tree.getClass.getSimpleName match {
      case "Empty" ⇒
        RefTree.Ref(tree, Seq(measure)).rename("FingerTree.Empty")
      case "Single" ⇒
        val a = digitRefTree[V, A](tree.privateField[AnyRef]("a"), depth - 1)
        RefTree.Ref(tree, Seq(measure, a)).rename("FingerTree.Single")
      case "Deep" ⇒
        val prefix = digitRefTree[V, A](tree.privateField[AnyRef]("prefix"), depth)
        val subtree = fingerTreeRefTree[V, A](tree.privateField[AnyRef]("tree"), depth + 1)
        val suffix = digitRefTree[V, A](tree.privateField[AnyRef]("suffix"), depth)
        RefTree.Ref(tree, Seq(measure, prefix, subtree, suffix)).rename("FingerTree.Deep")
    }
  }

  private def digitRefTree[V: ToRefTree, A: ToRefTree](digit: AnyRef, depth: Int): RefTree = {
    if (depth == 0) {
      // this is just A
      digit.asInstanceOf[A].refTree
    } else {
      // this is Digit[V, Digit[V, ... Digit[V, A]]]
      val measure = digit.privateField[V]("measure").refTree
      val childCount = Map(
        "One" → 1, "Two" → 2,
        "Three" → 3, "Four" → 4
      )(digit.getClass.getSimpleName)
      val children = Seq.tabulate(childCount) { i ⇒
        val child = digit.privateField[AnyRef](s"a${i + 1}")
        digitRefTree[V, A](child, depth - 1)
      }
      RefTree.Ref(digit, measure +: children).rename(s"FingerTree.${digit.getClass.getSimpleName}")
    }
  }

  implicit def `FingerTree RefTree`[V: ToRefTree, A: ToRefTree]: ToRefTree[FingerTree[V, A]] =
    ToRefTree[FingerTree[V, A]](fingerTreeRefTree[V, A](_, depth = 1))
}
