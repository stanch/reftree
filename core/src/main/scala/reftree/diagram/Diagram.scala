package reftree.diagram

import reftree.core._
import com.softwaremill.quicklens._

sealed trait Diagram {
  def fragments: Seq[Diagram.Single]

  def +(that: Diagram): Diagram =
    Diagram.Multiple(this.fragments ++ that.fragments)

  def withoutCaptions: Diagram =
    Diagram.Multiple(fragments.map(_.copy(caption = None)))

  def withoutAnchors: Diagram =
    Diagram.Multiple(fragments.map(_.copy(anchorId = None)))

  def toNamespace(name: String): Diagram =
    Diagram.Multiple(fragments.modify(_.each.namespace).using(name +: _))
}

object Diagram {
  case class Single(
    tree: RefTree,
    caption: Option[String] = None,
    colorIndex: Option[Int] = None,
    anchorId: Option[String] = None,
    namespace: Seq[String] = Seq.empty
  ) extends Diagram {
    def fragments = Seq(this)
    def withCaption(caption: String) = copy(caption = Some(caption))
    def withColor(index: Int) = copy(colorIndex = Some(index))
    def withAnchor(id: String) = copy(anchorId = Some(id))
  }

  case class Multiple(fragments: Seq[Diagram.Single]) extends Diagram

  def empty: Diagram = Multiple(Seq.empty)

  def apply[A: ToRefTree](value: A) =
    Single(value.refTree)

  def sourceCodeCaption[A: ToRefTree](value: sourcecode.Text[A]) =
    Single(value.value.refTree).withCaption(value.source)

  def toStringCaption[A: ToRefTree](value: A) =
    Single(value.refTree).withCaption(value.toString)
}
