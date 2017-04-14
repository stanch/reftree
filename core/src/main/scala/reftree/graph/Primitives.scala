package reftree.graph

import reftree.core.RefTree
import reftree.geometry.Color
import reftree.graph.Attr.AttrSyntax

object Primitives {
  // Always having a background simplifies the interpolation of highlights.
  val defaultBackground = Color.fromRgbaString("#ffffff00")

  private def namespaced(id: String, namespace: Seq[String]) =
    s"${namespace.mkString("/")}-$id"

  def caption(caption: String, tree: RefTree, color: Color, namespace: Seq[String]): Seq[GraphStatement] = {
    val captionNodeId = namespaced(s"${tree.id}-caption-${caption.hashCode}", namespace)
    val captionNode = Node(
      captionNodeId,
      "label" := <i>{ caption }</i>,
      "id" := captionNodeId,
      "fontcolor" := color.saturate(0.8).lighten(0.8).toRgbaString
    )
    val captionEdgeId = s"$captionNodeId-${tree.id}"
    val captionEdge = Edge(
      NodeId(captionNodeId).south,
      NodeId(namespaced(tree.id, namespace)).withPort("n").north,
      "id" := captionEdgeId,
      "color" := color.saturate(0.8).lighten(0.8).toRgbaString
    )
    Seq(captionNode, captionEdge)
  }

  def node(tree: RefTree, color: Color, anchorId: Option[String], namespace: Seq[String]): Node = {
    val background = (if (tree.highlight) color.opacify(0.2) else defaultBackground).toRgbaString
    val labelContent = tree match {
      case ref: RefTree.Ref ⇒
        val title = <td port="n">{ ref.name }</td>
        val cells = ref.children.zipWithIndex map { case (c, i) ⇒ cell(c, i, color) }
        <tr>{ title }{ cells }</tr>
      case _ ⇒
        val title = <td port="n">{ cellLabel(tree) }</td>;
        <tr>{ title }</tr>
    }
    val label = <table
      cellspacing="0" cellpadding="6" cellborder="0"
      columns="*" bgcolor={ background } style="rounded">{ labelContent }</table>
    val tooltipAttribute = anchorId.map(a ⇒ "tooltip" := s"anchor-$a").toSeq
    val id = namespaced(tree.id, namespace)
    Node(
      id,
      "id" := id,
      "label" := label,
      "color" := color.toRgbaString,
      "fontcolor" := color.toRgbaString
    ).addAttrs(tooltipAttribute: _*)
  }

  private def cellLabel(tree: RefTree): xml.Node = tree match {
    case _ if tree.elide ⇒ xml.EntityRef("hellip")
    case RefTree.Val(value: Int, Some(RefTree.Val.Bin), _, _) ⇒ xml.Text(value.toBinaryString)
    case RefTree.Val(value, _, _, _) ⇒ xml.Text(value.toString.replace(" ", "_"))
    case _: RefTree.Null ⇒ xml.EntityRef("empty")
    case RefTree.Ref(_, id, _, _, _) ⇒ xml.EntityRef("middot")
  }

  private def cell(tree: RefTree, i: Int, color: Color): xml.Node = {
    val label = cellLabel(tree)
    val port = tree match {
      case RefTree.Ref(_, id, _, _, _) ⇒ Some(xml.Text(s"$id-$i"))
      case _ ⇒ None
    }
    val background = ((tree, tree.highlight) match {
      case (_, false) | (RefTree.Ref(_, _, _, _, false), _) ⇒ defaultBackground
      case _ ⇒ color.opacify(0.25)
    }).toRgbaString
    <td port={ port } bgcolor={ background }>{ label }</td>
  }

  def edge(id: String, tree: RefTree, i: Int, color: Color, namespace: Seq[String]): Option[Edge] =
    tree match {
      case RefTree.Ref(_, refId, _, _, _) ⇒
        val sourceId = namespaced(id, namespace)
        val targetId = namespaced(refId, namespace)
        val edgeId = namespaced(s"$id-$i-$refId", namespace)
        Some(Edge(
          NodeId(sourceId).withPort(s"$refId-$i").south,
          NodeId(targetId).withPort("n").north,
          "id" := edgeId,
          "color" := color.toRgbaString
        ))
      case _ ⇒ None
    }
}
