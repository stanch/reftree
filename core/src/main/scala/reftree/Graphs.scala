package reftree

import reftree.Diagram.{SequenceRenderingOptions, RenderingOptions}
import uk.co.turingatemyhamster.graphvizs.dsl._

object Graphs {
  private def treeId(tree: RefTree) = tree match {
    case RefTree.Ref(_, id, _, _) ⇒ id
    case RefTree.Val(value, _, _) ⇒ value.toString
    case _: RefTree.Null ⇒ "null"
    case _: RefTree.Elided ⇒ "elided"
  }

  private def label(tree: LabeledRefTree): Seq[Statement] = {
    val id = treeId(tree.tree)
    val labelNodeId = s"$id-label"
    Seq(
      labelNodeId :| AttributeAssignment("label", ID.Identifier(s"<<i>${tree.label}</i>>")),
      NodeId(labelNodeId, Some(Port(None, Some(CompassPt.S)))) -->
        NodeId(id, Some(Port(Some("n"), Some(CompassPt.N))))
    )
  }

  private def node(tree: RefTree, color: String, options: RenderingOptions): NodeStatement = {
    val id = treeId(tree)
    val highlight = if (tree.highlight) s"""bgcolor="${options.highlightColor}"""" else ""
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
    id :| ("id" := id, labelAttribute, "color" := color, "fontcolor" := color)
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

  private def link(id: String, tree: RefTree, i: Int, color: String): Option[EdgeStatement] = tree match {
    case RefTree.Ref(_, linkId, _, _) ⇒ Some(
      NodeId(id, Some(Port(Some(s"$linkId-$i"), Some(CompassPt.S)))) -->
        NodeId(linkId, Some(Port(Some("n"), Some(CompassPt.N)))) :|
        ("id" := s"$id-$i-$linkId", "color" := color)
    )
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

  def graph(options: RenderingOptions)(trees: Seq[LabeledRefTree]): Graph = {
    val graphAttrs = "graph" :| ("ranksep" := options.verticalSpacing)
    val nodeAttrs = "node" :| ("shape" := "plaintext", "fontname" := "consolas")
    val edgeAttrs = "edge" :| ("arrowsize" := "0.7")

    val labels = if (options.labels) trees.flatMap(label) else Seq.empty

    val statements: Seq[Statement] = Seq(graphAttrs, nodeAttrs, edgeAttrs) ++ labels ++ {
      def inner(tree: RefTree, color: String, depth: Int): Seq[Statement] = tree match {
        case r @ RefTree.Ref(_, id, children, _) ⇒
          Seq(node(r, color, options)) ++
            children.flatMap(inner(_, color, depth + 1)) ++
            children.zipWithIndex.flatMap { case (c, i) ⇒ link(id, c, i, color) }
        case _ if depth == 0 ⇒
          Seq(node(tree, color, options))
        case _ ⇒
          Seq.empty
      }
      trees.map(_.tree).zipWithIndex.flatMap {
        case (tree, i) ⇒ inner(tree, options.palette(i % options.palette.length), depth = 0)
      }
    }

    def sharing[A](sequence: Seq[A]) =
      if (options.commonNodesBelongToLastTree) sequence.reverse.distinct.reverse else sequence.distinct
    val distinct = sharing(statements.map(ColorlessStatement)).map(_.s)
    NonStrictDigraph("g", distinct: _*)
  }

  private def accentuateDiff(graph1: Graph, graph2: Graph, accentColor: String) = {
    val previous = graph1.statements.map(ColorlessStatement).toSet
    graph2.copy(statements = graph2.statements map {
      case n: NodeStatement if !previous(ColorlessStatement(n)) ⇒
        n.copy(attributes = n.attributes.map(_ ++ Seq("color" := accentColor, "fontcolor" := accentColor)))
      case e: EdgeStatement if !previous(ColorlessStatement(e)) ⇒
        e.copy(attributes = e.attributes.map(_ ++ Seq("color" := accentColor)))
      case s ⇒ s
    })
  }

  def graphFrames(options: SequenceRenderingOptions)(trees: Seq[LabeledRefTree]): Seq[Graph] = {
    val prefix = Seq.fill(options.onionSkinLayers)(trees.head)
    val frames = (prefix ++ trees).sliding(options.onionSkinLayers + 1).map(graph(options)).toSeq
    if (!options.diffAccent) frames else {
      val accentuated = frames.sliding(2).map {
        case Seq(prev, next) ⇒ accentuateDiff(prev, next, options.accentColor)
      }.toSeq
      frames.head +: accentuated
    }
  }
}
