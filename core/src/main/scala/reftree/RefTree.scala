package reftree

import scala.collection.immutable.CollectionInstances

sealed trait RefTree {
  def id: String
  def highlight: Boolean
}

object RefTree {
  case class Val(value: AnyVal, hint: Option[Val.Hint], highlight: Boolean) extends RefTree {
    def id = value.toString
  }
  object Val {
    sealed trait Hint
    case object Bin extends Hint
    def apply(value: AnyVal): Val = Val(value, None, highlight = false)
  }

  case class Null(highlight: Boolean = false) extends RefTree {
    def id = "null"
  }
  case class Elided(highlight: Boolean = false) extends RefTree {
    def id = "elided"
  }

  case class Ref(name: String, id: String, children: Seq[RefTree], highlight: Boolean) extends RefTree
  object Ref {
    def apply(value: AnyRef, children: Seq[RefTree]): Ref = Ref(
      // getSimpleName sometimes does not work, see https://issues.scala-lang.org/browse/SI-5425
      try { value.getClass.getSimpleName }
      catch { case _: InternalError ⇒ value.getClass.getName.replaceAll("^.+\\$", "") },
      s"${value.getClass.getName}${System.identityHashCode(value)}",
      children,
      highlight = false
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

trait ToRefTree[-A] { self ⇒
  def refTree(value: A): RefTree

  def suppressField(index: Int) = new ToRefTree[A] {
    def refTree(value: A) = self.refTree(value) match {
      case r: RefTree.Ref ⇒ r.copy(children = r.children.updated(index, RefTree.Elided()))
      case t ⇒ t
    }
  }
}

object ToRefTree extends CollectionInstances with GenericInstances {
  implicit def `AnyVal RefTree`: ToRefTree[AnyVal] = new ToRefTree[AnyVal] {
    def refTree(value: AnyVal) = RefTree.Val(value)
  }

  implicit def `String RefTree`: ToRefTree[String] = new ToRefTree[String] {
    def refTree(value: String) = RefTree.Ref(value, value.map(RefTree.Val.apply))
  }
}
