package reftree.graph

import reftree.render.RenderingOptions
import reftree.diagram.{Animation, Diagram}
import reftree.core.RefTree
import uk.co.turingatemyhamster.graphvizs.dsl._

object Graphs {
  private def namespaced(id: String, namespace: Seq[String]) =
    s"${namespace.mkString("/")}-$id"

  private def caption(caption: String, tree: RefTree, namespace: Seq[String]): Seq[Statement] = {
    val captionNodeId = namespaced(s"${tree.id}-caption-${caption.hashCode}", namespace)
    val captionNode = captionNodeId :| (
      AttributeAssignment("label", ID.Identifier(s"<<i>$caption</i>>")),
      "id" := captionNodeId
    )
    val captionEdgeId = namespaced(s"$captionNodeId-${tree.id}", namespace)
    val captionEdge =
      NodeId(captionNodeId, Some(Port(None, Some(CompassPt.S)))) -->
      NodeId(namespaced(tree.id, namespace), Some(Port(Some("n"), Some(CompassPt.N)))) :|
      ("id" := captionEdgeId)
    Seq(captionNode, captionEdge)
  }

  private def node(
    tree: RefTree,
    color: String,
    highlightColor: String,
    anchorId: Option[String],
    namespace: Seq[String]
  ): NodeStatement = {
    val highlight = if (tree.highlight) s"""bgcolor="$highlightColor"""" else ""
    val style = s"""style="rounded" cellspacing="0" cellpadding="6" cellborder="0" columns="*" $highlight"""
    val label = tree match {
      case ref: RefTree.Ref ⇒
        val title = s"""<td port="n">${ref.name}</td>"""
        val cells = ref.children.zipWithIndex map { case (c, i) ⇒ cell(c, i) }
        s"""<<table $style><tr>${(title +: cells).mkString}</tr></table>>"""
      case _ ⇒
        val title = s"""<td port="n">${cellLabel(tree)}</td>"""
        s"""<<table $style><tr>$title</tr></table>>"""
    }
    val labelAttribute = AttributeAssignment("label", ID.Identifier(label))
    val tooltipAttribute = anchorId.map(a ⇒ "tooltip" := s"anchor-$a").toSeq
    val id = namespaced(tree.id, namespace)
    id :| ("id" := id, labelAttribute, "color" := color, "fontcolor" := color) :| (tooltipAttribute: _*)
  }

  private def cellLabel(tree: RefTree): String = tree match {
    case RefTree.Val(value: Int, Some(RefTree.Val.Bin), _) ⇒ value.toBinaryString
    case RefTree.Val(value, _, _) ⇒ value.toString.replace(" ", "_")
    case _: RefTree.Null ⇒ "&empty;"
    case _: RefTree.Elided ⇒ "&hellip;"
    case RefTree.Ref(_, id, _, _) ⇒ "&middot;"
  }

  private def cell(tree: RefTree, i: Int): String = {
    val label = cellLabel(tree)
    val port = tree match {
      case RefTree.Ref(_, id, _, _) ⇒ s"""port="$id-$i""""
      case _ ⇒ ""
    }
    val highlight = (tree, tree.highlight) match {
      case (_, false) | (_: RefTree.Ref, _) ⇒ ""
      case _ ⇒ """bgcolor="bisque""""
    }
    s"""<td $port $highlight>$label</td>"""
  }

  private def edge(
    id: String,
    tree: RefTree,
    i: Int,
    color: String,
    namespace: Seq[String]
  ): Option[EdgeStatement] = tree match {
    case RefTree.Ref(_, refId, _, _) ⇒
      val sourceId = namespaced(id, namespace)
      val targetId = namespaced(refId, namespace)
      val edgeId = namespaced(s"$id-$i-$refId", namespace)
      Some {
        NodeId(sourceId, Some(Port(Some(s"$refId-$i"), Some(CompassPt.S)))) -->
          NodeId(targetId, Some(Port(Some("n"), Some(CompassPt.N)))) :|
          ("id" := edgeId, "color" := color)
      }
    case _ ⇒ None
  }

  private case class ColorlessStatement(s: Statement) {
    override def equals(other: Any) = other match {
      case ColorlessStatement(o) ⇒ (s, o) match {
        case (n1: NodeStatement, n2: NodeStatement) ⇒ n1.copy(attributes = None) == n2.copy(attributes = None)
        case (e1: EdgeStatement, e2: EdgeStatement) ⇒ e1.copy(attributes = None) == e2.copy(attributes = None)
        case (x, y) ⇒ x == y
      }
      case _ ⇒ false
    }

    override def hashCode() = s match {
      case n: NodeStatement ⇒ n.copy(attributes = None).hashCode()
      case e: EdgeStatement ⇒ e.copy(attributes = None).hashCode()
      case x ⇒ x.hashCode()
    }
  }

  private def graphAttributes(options: RenderingOptions): Seq[Statement] = Seq(
    "graph" :| ("ranksep" := options.verticalSpacing),
    "node" :| ("shape" := "plaintext", "fontname" := "consolas"),
    "edge" :| ("arrowsize" := "0.7", "color" := "#000000")
  )

  private def graphStatements(options: RenderingOptions)(diagram: Diagram): Seq[Statement] = {
    def inner(
      tree: RefTree,
      color: String,
      anchorId: Option[String],
      namespace: Seq[String],
      depth: Int
    ): Seq[Statement] = tree match {
      case r @ RefTree.Ref(_, id, children, _) ⇒
        Seq(node(r, color, options.highlightColor, anchorId, namespace)) ++
          children.flatMap(inner(_, color, None, namespace, depth + 1)) ++
          children.zipWithIndex.flatMap { case (c, i) ⇒ edge(id, c, i, color, namespace) }
      case _ if depth == 0 ⇒
        Seq(node(tree, color, options.highlightColor, anchorId, namespace))
      case _ ⇒
        Seq.empty
    }

    val spareColorIndices = Iterator.from(0).filterNot(diagram.fragments.flatMap(_.colorIndex).toSet)
    val colorIndices = diagram.fragments.map(_.colorIndex.getOrElse(spareColorIndices.next()))

    (diagram.fragments zip colorIndices) flatMap {
      case (fragment, i) ⇒
        val color = options.palette(i % options.palette.length)
        fragment.caption.toSeq.flatMap(caption(_, fragment.tree, fragment.namespace)) ++
          inner(fragment.tree, color, fragment.anchorId, fragment.namespace, depth = 0)
    }
  }

  private def deduplicateLeft(statements: Seq[Statement]): Seq[Statement] =
    statements.map(ColorlessStatement).distinct.map(_.s)

  private def deduplicateRight(statements: Seq[Statement]): Seq[Statement] =
    statements.map(ColorlessStatement).reverse.distinct.reverse.map(_.s)

  def graph(options: RenderingOptions)(diagram: Diagram): Graph = {
    val statements = graphAttributes(options) ++
      deduplicateLeft(graphStatements(options)(diagram))
    NonStrictDigraph("diagram", statements: _*)
  }

  def graphs(options: RenderingOptions, onionSkinLayers: Int)(animation: Animation): Seq[Graph] = {
    val prefix = Seq.fill(onionSkinLayers)(animation.diagrams.head)
    (prefix ++ animation.diagrams).sliding(onionSkinLayers + 1).toSeq map { diagrams ⇒
      val onionSkin = diagrams.init.map(_.withoutAnchors)
      val statementLayers = (onionSkin :+ diagrams.last).map(graphStatements(options))
      val statements = graphAttributes(options) ++
        deduplicateRight(statementLayers.flatMap(deduplicateLeft))
      NonStrictDigraph("diagram", statements: _*)
    }
  }
}
