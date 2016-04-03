package reftree

import java.nio.file.{Paths, Path}

import uk.co.turingatemyhamster.graphvizs.dsl._
import uk.co.turingatemyhamster.graphvizs.exec._

import scala.sys.process.{Process, ProcessIO}

case class DotPlotter(output: Path = Paths.get("graph.png"), verticalSpacing: Double = 0.8) {
  private def label(tree: LabeledRefTree): Seq[Statement] = tree match {
    case LabeledRefTree(label, ref: RefTree.Ref) ⇒
      val labelNodeId = s"${ref.id}-label"
      Seq(
        labelNodeId :| ("shape" := "plaintext", "label" := label, "fontname" := "consolas italic"),
        NodeId(labelNodeId, Some(Port(None, Some(CompassPt.S)))) -->
        NodeId(ref.id, Some(Port(Some("n"), Some(CompassPt.N))))
      )
    case _ ⇒ Seq.empty
  }

  private def node(ref: RefTree.Ref, color: String): NodeStatement = {
    val cells = ref.children.zipWithIndex map { case (c, i) ⇒ cell(c, i) }
    val label = (s"<n>${ref.name}" +: cells).mkString("|")
    val highlight = if (ref.highlight) Some("penwidth" := 2) else None
    ref.id :| ("label" := label, "color" := color, "fontcolor" := color) :| (highlight.toSeq: _*)
  }

  private def cell(tree: RefTree, i: Int): String = {
    // TODO: use HTML nodes to do proper highlighting
    val label = tree match {
      case RefTree.Val(value: Int, Some(RefTree.Val.Bin), _) ⇒ value.toBinaryString
      case RefTree.Val(value, _, _) ⇒ value.toString.replace(" ", "_")
      case _: RefTree.Null ⇒ "&empty;"
      case _: RefTree.Elided ⇒ "&hellip;"
      case RefTree.Ref(_, id, _, _) ⇒ s"<$id-$i>&middot;"
    }
    if (tree.highlight && !tree.isInstanceOf[RefTree.Ref]) s"[$label]" else label
  }

  private def link(id: String, tree: RefTree, i: Int, color: String): Option[EdgeStatement] = tree match {
    case RefTree.Ref(_, linkId, _, _) ⇒ Some(
      NodeId(id, Some(Port(Some(s"$linkId-$i"), Some(CompassPt.S)))) -->
      NodeId(linkId, Some(Port(Some("n"), Some(CompassPt.N)))) :| ("color" := color)
    )
    case _ ⇒ None
  }

  case class ColorlessStatement(s: Statement) {
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

  def plot(trees: LabeledRefTree*) = {
    val graphAttrs = "graph" :| ("ranksep" := verticalSpacing)
    val nodeAttrs = "node" :| ("shape" := "Mrecord", "fontname" := "consolas")
    val statements: Seq[Statement] = Seq(graphAttrs, nodeAttrs) ++ {
      def inner(tree: RefTree, color: String): Seq[Statement] = tree match {
        case r @ RefTree.Ref(_, id, children, _) ⇒
          Seq(node(r, color)) ++
            children.flatMap(inner(_, color)) ++
            children.zipWithIndex.flatMap { case (c, i) ⇒ link(id, c, i, color) }
        case _ ⇒
          Seq.empty
      }
      val colors = Array("dodgerblue4", "forestgreen", "coral3")
      trees.flatMap(label) ++ trees.map(_.tree).zipWithIndex.flatMap {
        case (tree, i) ⇒ inner(tree, colors(i % colors.length))
      }
    }

    val distinct = statements.map(ColorlessStatement).distinct.map(_.s)
    val graph = NonStrictDigraph("g", distinct: _*)
    write(graph)
  }

  private def write(graph: Graph) = {
    val opts = DotOpts(
      Some(DotLayout.dot), Some(DotFormat.png), Some(output.toFile),
      Seq("-Gdpi=300")
    )
    val process = Process(dotBinary.getAbsolutePath, opts.generate)
    val io = new ProcessIO(
      GraphInputHandler.handle(graph), Function.const(()), Function.const(()), false
    )
    process run io
    ()
  }
}
