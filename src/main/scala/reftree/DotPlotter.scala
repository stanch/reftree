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
    val cells = ref.children.map(cell)
    val label = (s"<n>${ref.name}" +: cells).mkString("|")
    ref.id :| ("label" := label, "color" := color, "fontcolor" := color)
  }

  private def cell(tree: RefTree): String = tree match {
    case RefTree.Val(value: Int, Some(RefTree.Val.Bin)) ⇒ value.toBinaryString
    case RefTree.Val(value, _) ⇒ value.toString.replace(" ", "_")
    case RefTree.Null ⇒ "&empty;"
    case RefTree.Undefined ⇒ "&#9617;"
    case RefTree.Ref(_, id, _) ⇒ s"<$id>&middot;"
  }

  private def link(id: String, tree: RefTree, color: String): Option[EdgeStatement] = tree match {
    case RefTree.Ref(_, linkId, _) ⇒ Some(
      NodeId(id, Some(Port(Some(linkId), Some(CompassPt.S)))) -->
      NodeId(linkId, Some(Port(Some("n"), Some(CompassPt.N)))) :| ("color" := color)
    )
    case _ ⇒ None
  }

  def plot(trees: LabeledRefTree*) = {
    trees.foreach(t ⇒ println(t.label))
    val graphAttrs = "graph" :| ("ranksep" := verticalSpacing)
    val nodeAttrs = "node" :| ("shape" := "Mrecord", "fontname" := "consolas")
    val statements: Seq[Statement] = Seq(graphAttrs, nodeAttrs) ++ {
      def inner(tree: RefTree, color: String): Seq[Statement] = tree match {
        case r @ RefTree.Ref(_, id, children) ⇒
          Seq(node(r, color)) ++ children.flatMap(inner(_, color)) ++ children.flatMap(link(id, _, color))
        case _ ⇒
          Seq.empty
      }
      val colors = Array("dodgerblue4", "forestgreen", "coral3")
      trees.flatMap(label) ++ trees.map(_.tree).zipWithIndex.reverse.flatMap {
        case (tree, i) ⇒ inner(tree, colors(i % colors.length))
      }
    }

    val graph = StrictDigraph("g", statements: _*)
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
