package reftree.graph

import reftree.core.RefTree
import reftree.geometry.Color
import reftree.dot._
import reftree.dot.html._

object Primitives {
  // Always having a background simplifies the interpolation of highlights.
  val defaultBackground = Color.fromRgbaString("#ffffff00")

  private def namespaced(id: String, namespace: Seq[String]) =
    s"${namespace.mkString("/")}-$id"

  def caption(caption: String, tree: RefTree, color: Color, namespace: Seq[String]): Seq[GraphStatement] = {
    val captionNodeId = namespaced(s"${tree.id}-caption-${caption.hashCode}", namespace)
    val captionNode = Node(
      captionNodeId,
      Italic(caption),
      Node.Attrs(fontColor = Some(color.saturate(0.8).lighten(0.8)))
    )
    val captionEdgeId = s"$captionNodeId-${tree.id}"
    val captionEdge = Edge(
      NodeId(captionNodeId).south,
      NodeId(namespaced(tree.id, namespace)).withPort("n").north,
      captionEdgeId,
      Edge.Attrs(color = Some(color.saturate(0.8).lighten(0.8)))
    )
    Seq(captionNode, captionEdge)
  }

  def node(tree: RefTree, color: Color, anchorId: Option[String], namespace: Seq[String]): Node = {
    val background = if (tree.highlight) color.opacify(0.2) else defaultBackground
    val labelContent: Seq[Row] = tree match {
      case ref: RefTree.Ref ⇒
        val title = Cell(
          Plain(ref.name),
          Cell.Attrs(port = Some("n"), rowSpan = Some(2))
        )
        Seq(true, false).map { firstRow ⇒
          ref.children.zipWithIndex flatMap { case (c, i) ⇒ cell(c, i, color, firstRow) }
        } match {
          case Seq(row1, Seq()) ⇒ Seq(RowContent(title +: row1))
          case Seq(row1, row2) ⇒ Seq(RowContent(title +: row1), RowDivider, RowContent(row2))
        }
      case _ ⇒
        val title = Cell(cellLabel(tree), Cell.Attrs(port = Some("n")))
        Seq(RowContent(Seq(title)))
    }
    val label = Table(labelContent, Table.Attrs(
      cellSpacing = Some(0), cellPadding = Some(6), cellBorder = Some(0), columns = Some("*"),
      bgColor = Some(background), style = Some("rounded")
    ))
    val tooltip = anchorId.map(a ⇒ s"anchor-$a")
    val id = namespaced(tree.id, namespace)
    Node(id, label, Node.Attrs(fontColor = Some(color), color = Some(color), tooltip = tooltip))
  }

  private def cellLabel(tree: RefTree, elideRefs: Boolean = false): Html = tree match {
    case RefTree.Val(value: Int, Some(RefTree.Val.Bin), _) ⇒ Plain(value.toBinaryString)
    case RefTree.Val(value: Int, Some(RefTree.Val.Hex), _) ⇒ Plain(value.toHexString)
    case RefTree.Val(value, _, _) ⇒ Plain(value.toString.replace(" ", "_"))
    case _: RefTree.Null ⇒ Raw("&empty;")
    case RefTree.Ref(_, id, _, _) ⇒
      if (elideRefs) Raw("&hellip;") else Raw("&middot;")
  }

  private def cell(field: RefTree.Ref.Field, i: Int, color: Color, firstRow: Boolean): Option[Cell] =
    if (!firstRow && field.name.isEmpty) None else Some {
      val span = if (firstRow && field.name.isEmpty) Some(2) else None
      val label = (firstRow, field.name) match {
        case (true, Some(name)) ⇒ Italic(name)
        case _ ⇒ cellLabel(field.value, field.elideRefs)
      }
      val port = (firstRow, field.name, field.value) match {
        case (true, Some(_), _) ⇒ None
        case (_, _, _: RefTree.Ref) ⇒ Some(i.toString)
        case _ ⇒ None
      }
      val background = (field.value, field.value.highlight) match {
        case (_, false) | (_: RefTree.Ref, _) ⇒ defaultBackground
        case _ ⇒ color.opacify(0.25)
      }
      Cell(label, Cell.Attrs(port, span, Some(background)))
    }

  def edge(id: String, tree: RefTree, i: Int, color: Color, namespace: Seq[String]): Option[Edge] =
    tree match {
      case RefTree.Ref(_, refId, _, _) ⇒
        val sourceId = namespaced(id, namespace)
        val targetId = namespaced(refId, namespace)
        val edgeId = namespaced(s"$id-$i-$refId", namespace)
        Some(Edge(
          NodeId(sourceId).withPort(i.toString).south,
          NodeId(targetId).withPort("n").north,
          edgeId,
          Edge.Attrs(color = Some(color))
        ))
      case _ ⇒ None
    }
}
