package reftree.core

import com.softwaremill.quicklens._

import scala.annotation.implicitNotFound
import scala.collection.immutable.CollectionInstances

sealed trait RefTree {
  def id: String
  def highlight: Boolean

  def withHighlight(highlight: Boolean) = this match {
    case tree: RefTree.Val ⇒ tree.copy(highlight = highlight)
    case tree: RefTree.Null ⇒ tree.copy(highlight = highlight)
    case tree: RefTree.Elided ⇒ tree.copy(highlight = highlight)
    case tree: RefTree.Ref ⇒ tree.copy(highlight = highlight)
  }
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

  case class Ref private (
    name: String,
    id: String,
    children: Seq[RefTree],
    highlight: Boolean
  ) extends RefTree {
    def rename(name: String) = copy(name = name)
  }

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

@implicitNotFound("To render a diagram for type ${A}, implement an instance of reftree.core.ToRefTree[${A}]")
trait ToRefTree[-A] { self ⇒
  def refTree(value: A): RefTree

  def highlightField(index: Int) = ToRefTree[A] { value ⇒
    self.refTree(value).modify(_.when[RefTree.Ref].children.at(index)).using(_.withHighlight(true))
  }

  def suppressField(index: Int) = ToRefTree[A] { value ⇒
    self.refTree(value).modify(_.when[RefTree.Ref].children.at(index)).setTo(RefTree.Elided())
  }
}

object ToRefTree extends CollectionInstances with GenericInstances {
  def apply[A](toRefTree: A ⇒ RefTree): ToRefTree[A] = new ToRefTree[A] {
    def refTree(value: A) = toRefTree(value)
  }

  implicit def `AnyVal RefTree`: ToRefTree[AnyVal] = ToRefTree[AnyVal](RefTree.Val.apply)

  implicit def `String RefTree`: ToRefTree[String] = ToRefTree[String] { value ⇒
    RefTree.Ref(value, value.map(RefTree.Val.apply))
  }
}
