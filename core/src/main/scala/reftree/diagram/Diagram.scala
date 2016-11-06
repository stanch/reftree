package reftree.diagram

import reftree.core._
import com.softwaremill.quicklens._

/**
 * The central type for producing diagrams
 *
 * A Diagram can be either a [[Diagram.Single]] (a diagram of a single [[RefTree]]),
 * or [[Diagram.Multiple]] — a diagram containing several trees.
 *
 * Diagrams can be combined using the `+` operator.
 *
 * Each diagram is associated with a hierarchical namespace.
 * This namespace scopes the ids of the trees in the diagram.
 * Put simply, two [[List]] diagrams will share the same [[Nil]] tree node
 * if they are in the same namespace, and will each have its own [[Nil]] node otherwise.
 *
 * Diagrams can have “anchors” which prevent their root nodes from moving
 * between adjacent animation frames. For more details see [[Animation]].
 *
 * Usage examples:
 * {{{
 *   // no caption
 *   Diagram(Queue(1))
 *
 *   // automatically set caption to "Queue(1) :+ 2"
 *   Diagram.sourceCodeCaption(Queue(1) :+ 2)
 *
 *   // use toString to get the caption, i.e. "Queue(1, 2)"
 *   Diagram.toStringCaption(Queue(1) :+ 2)
 *
 *   // merge two diagrams, set captions manually
 *   Diagram(Queue(1)).withCaption("one") + Diagram(Queue(2)).withCaption("two")
 *
 *   // isolate each diagram in its own namespace
 *   Diagram(Queue(1)).toNamespace("one") + Diagram(Queue(2)).toNamespace("two")
 * }}}
 */
sealed trait Diagram {
  /** The fragments of this diagram */
  def fragments: Seq[Diagram.Single]

  /** Combine with another diagram */
  def +(that: Diagram): Diagram =
    Diagram.Multiple(this.fragments ++ that.fragments)

  /** Remove captions from all fragments */
  def withoutCaptions: Diagram =
    Diagram.Multiple(fragments.map(_.copy(caption = None)))

  /** Remove anchors from all fragments */
  def withoutAnchors: Diagram =
    Diagram.Multiple(fragments.map(_.copy(anchorId = None)))

  /** Put this diagram into a namespace with the given name (add one more hierarchy level) */
  def toNamespace(name: String): Diagram =
    Diagram.Multiple(fragments.modify(_.each.namespace).using(name +: _))
}

object Diagram {
  /**
   * A diagram with a single tree
   *
   * @param tree the tree to visualize
   * @param caption the caption of the diagram
   * @param colorIndex the palette index of the color to use
   * @param anchorId the anchor, if any (for anchoring between animation frames)
   * @param namespace a sequence of strings defining the namespace hierarchy
   */
  case class Single(
    tree: RefTree,
    caption: Option[String] = None,
    colorIndex: Option[Int] = None,
    anchorId: Option[String] = None,
    namespace: Seq[String] = Seq.empty
  ) extends Diagram {
    def fragments = Seq(this)

    /** Add a caption */
    def withCaption(caption: String) = copy(caption = Some(caption))

    /** Force a particular color to be used by specifying its palette index */
    def withColor(index: Int) = copy(colorIndex = Some(index))

    /** Add an anchor to reduce movement for the root node between animation frames */
    def withAnchor(id: String) = copy(anchorId = Some(id))
  }

  /** A diagram comprised of several single diagrams */
  case class Multiple(fragments: Seq[Diagram.Single]) extends Diagram

  /** An empty diagram */
  def empty: Diagram = Multiple(Seq.empty)

  /** Create a diagram for a value */
  def apply[A: ToRefTree](value: A) =
    Single(value.refTree)

  /** Create a diagram for a value, using its source code as the caption */
  def sourceCodeCaption[A: ToRefTree](value: sourcecode.Text[A]) =
    Single(value.value.refTree).withCaption(value.source)

  /** Create a diagram for a value, using its `toString` representation as the caption */
  def toStringCaption[A: ToRefTree](value: A) =
    Single(value.refTree).withCaption(value.toString)
}
