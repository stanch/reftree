package reftree.graph

import reftree.core.RefTree
import reftree.geometry.Color
import uk.co.turingatemyhamster.graphvizs.dsl._

object Primitives {
  // Always having a background simplifies the interpolation of highlights.
  val defaultBackground = Color.fromRgbaString("#ffffff00")

  private def namespaced(id: String, namespace: Seq[String]) =
    s"${namespace.mkString("/")}-$id"

  def caption(caption: String, tree: RefTree, color: Color, namespace: Seq[String]): Seq[Statement] = {
    val captionNodeId = namespaced(s"${tree.id}-caption-${caption.hashCode}", namespace)
    val captionNode = captionNodeId :| (
      AttributeAssignment("label", ID.Identifier(s"<<i>$caption</i>>")),
      "id" := captionNodeId,
      "fontcolor" := color.saturate(0.8).lighten(0.8).toRgbaString
    )
    val captionEdgeId = s"$captionNodeId-${tree.id}"
    val captionEdge =
      NodeId(captionNodeId, Some(Port(None, Some(CompassPt.S)))) -->
      NodeId(namespaced(tree.id, namespace), Some(Port(Some("n"), Some(CompassPt.N)))) :| (
        "id" := captionEdgeId,
        "color" := color.saturate(0.8).lighten(0.8).toRgbaString
      )
    Seq(captionNode, captionEdge)
  }

  def node(tree: RefTree, color: Color, anchorId: Option[String], namespace: Seq[String]): NodeStatement = {
    val background = (if (tree.highlight) color.opacify(0.2) else defaultBackground).toRgbaString
    val style =
      s"""style="rounded" cellspacing="0" cellpadding="6" cellborder="0" columns="*" bgcolor="$background""""
    val label = tree match {
      case ref: RefTree.Ref ⇒
        val title = s"""<td port="n">${ref.name}</td>"""
        val cells = ref.children.zipWithIndex map { case (c, i) ⇒ cell(c, i, color) }
        s"""<<table $style><tr>${(title +: cells).mkString}</tr></table>>"""
      case _ ⇒
        val title = s"""<td port="n">${cellLabel(tree)}</td>"""
        s"""<<table $style><tr>$title</tr></table>>"""
    }
    val labelAttribute = AttributeAssignment("label", ID.Identifier(label))
    val tooltipAttribute = anchorId.map(a ⇒ "tooltip" := s"anchor-$a").toSeq
    val id = namespaced(tree.id, namespace)
    id :|
      ("id" := id, labelAttribute, "color" := color.toRgbaString, "fontcolor" := color.toRgbaString) :|
      (tooltipAttribute: _*)
  }

  private def cellLabel(tree: RefTree): String = tree match {
    case RefTree.Val(value: Int, Some(RefTree.Val.Bin), _) ⇒ value.toBinaryString
    case RefTree.Val(value, _, _) ⇒ value.toString.replace(" ", "_")
    case _: RefTree.Null ⇒ "&empty;"
    case _: RefTree.Elided ⇒ "&hellip;"
    case RefTree.Ref(_, id, _, _) ⇒ "&middot;"
  }

  private def cell(tree: RefTree, i: Int, color: Color): String = {
    val label = cellLabel(tree)
    val port = tree match {
      case RefTree.Ref(_, id, _, _) ⇒ s"""port="$id-$i""""
      case _ ⇒ ""
    }
    val background = ((tree, tree.highlight) match {
      case (_, false) | (_: RefTree.Ref, _) ⇒ defaultBackground
      case _ ⇒ color.opacify(0.25)
    }).toRgbaString
    s"""<td $port bgcolor="$background">$label</td>"""
  }

  def edge(id: String, tree: RefTree, i: Int, color: Color, namespace: Seq[String]): Option[EdgeStatement] =
    tree match {
      case RefTree.Ref(_, refId, _, _) ⇒
        val sourceId = namespaced(id, namespace)
        val targetId = namespaced(refId, namespace)
        val edgeId = namespaced(s"$id-$i-$refId", namespace)
        Some {
          NodeId(sourceId, Some(Port(Some(s"$refId-$i"), Some(CompassPt.S)))) -->
          NodeId(targetId, Some(Port(Some("n"), Some(CompassPt.N)))) :|
          ("id" := edgeId, "color" := color.toRgbaString)
        }
      case _ ⇒ None
    }
}
