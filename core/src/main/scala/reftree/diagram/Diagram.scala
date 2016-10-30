package reftree.diagram

import reftree.core._
import com.softwaremill.quicklens._

sealed trait Diagram {
  def fragments: Seq[Diagram.Single]

  def +(that: Diagram): Diagram =
    Diagram.Multiple(this.fragments ++ that.fragments)

  def withoutAnchors: Diagram =
    Diagram.Multiple(fragments.map(_.copy(anchorId = None)))

  def toNamespace(name: String): Diagram =
    Diagram.Multiple(fragments.modify(_.each.namespace).using(name +: _))
}

object Diagram {
  case class Single(
    tree: RefTree,
    label: Option[String] = None,
    colorIndex: Option[Int] = None,
    anchorId: Option[String] = None,
    namespace: Seq[String] = Seq.empty
  ) extends Diagram {
    def fragments = Seq(this)
    def withLabel(label: String) = copy(label = Some(label))
    def withColor(index: Int) = copy(colorIndex = Some(index))
    def withAnchor(id: String) = copy(anchorId = Some(id))
  }

  case class Multiple(fragments: Seq[Diagram.Single]) extends Diagram

  def empty: Diagram = Multiple(Seq.empty)

  def apply[A: ToRefTree](value: A) =
    Single(value.refTree)

  def sourceCodeLabel[A: ToRefTree](value: sourcecode.Text[A]) =
    Single(value.value.refTree).withLabel(value.source)

  def toStringLabel[A: ToRefTree](value: A) =
    Single(value.refTree).withLabel(value.toString)
}
