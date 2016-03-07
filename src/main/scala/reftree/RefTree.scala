package reftree

import scala.collection.immutable.CollectionInstances

sealed trait RefTree

object RefTree {
  case class Val(value: AnyVal, hint: Option[Val.Hint]) extends RefTree
  object Val {
    sealed trait Hint
    case object Bin extends Hint
    def apply(value: AnyVal): Val = Val(value, None)
  }

  case object Null extends RefTree
  case object Undefined extends RefTree

  case class Ref(name: String, id: String, children: Seq[RefTree]) extends RefTree
  object Ref {
    def apply(value: AnyRef, children: Seq[RefTree]): Ref = Ref(
      // getSimpleName sometimes does not work, see https://issues.scala-lang.org/browse/SI-5425
      try { value.getClass.getSimpleName }
      catch { case _: InternalError ⇒ value.getClass.getName.replaceAll("^.+\\$", "") },
      s"${value.getClass.getName}${System.identityHashCode(value)}",
      children
    )
  }
}

case class LabeledRefTree(label: String, tree: RefTree)

object LabeledRefTree {
  import scala.language.implicitConversions
  import scala.language.experimental.macros
  import scala.reflect.macros.blackbox

  implicit def fromTuple[A: ToRefTree](pair: (String, A)): LabeledRefTree =
    LabeledRefTree(pair._1, pair._2.refTree)

  implicit def fromValue[A](value: A)(implicit toRefTree: ToRefTree[A]): LabeledRefTree =
    macro fromValueMacro[A]

  def fromValueMacro[A](c: blackbox.Context)(value: c.Expr[A])(toRefTree: c.Expr[ToRefTree[A]]) = {
    import c.universe._
    val source = q"_root_.sourcecode.Text($value)"
    q"_root_.reftree.LabeledRefTree($source.source, $toRefTree.refTree($value))"
  }
}

trait ToRefTree[-A] {
  def refTree(value: A): RefTree
}

object ToRefTree extends CollectionInstances with GenericInstances {
  implicit def `AnyVal RefTree`: ToRefTree[AnyVal] = new ToRefTree[AnyVal] {
    def refTree(value: AnyVal) = RefTree.Val(value)
  }

  implicit def `String RefTree`: ToRefTree[String] = new ToRefTree[String] {
    def refTree(value: String) = RefTree.Ref(value, value.map(RefTree.Val.apply))
  }
}
