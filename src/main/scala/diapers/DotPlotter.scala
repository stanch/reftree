package diapers

import java.io.File

import uk.co.turingatemyhamster.graphvizs.dsl._
import uk.co.turingatemyhamster.graphvizs.exec._

object DotPlotter {
  def node(ref: Tree.Ref): NodeStatement = {
    val cells = ref.children.map(cell)
    val label = (s"<n>${ref.name}" +: cells).mkString("|")
    ref.id :| ("label" := label)
  }

  def cell(tree: Tree): String = tree match {
    case Tree.Val(value: Int, Some(Tree.Val.Hex)) ⇒ value.toBinaryString
    case Tree.Val(value, _) ⇒ value.toString.replace(" ", "_")
    case Tree.Ref(_, id, _) ⇒ s"<$id>&middot;"
  }

  def link(id: String, tree: Tree): Option[EdgeStatement] = tree match {
    case Tree.Val(_, _) ⇒ None
    case Tree.Ref(_, linkId, _) ⇒ Some(
      NodeId(id, Some(Port(Some(linkId), Some(CompassPt.S)))) -->
      NodeId(linkId, Some(Port(Some("n"), Some(CompassPt.N))))
    )
  }

  def plot(trees: Tree*) = {
    val graphAttrs = "graph" :| ("ranksep" := "0.8")
    val nodeAttrs = "node" :| ("shape" := "Mrecord")
    val statements: Seq[Statement] = Seq(graphAttrs, nodeAttrs) ++ {
      def inner(tree: Tree): Seq[Statement] = tree match {
        case r @ Tree.Ref(_, id, children) ⇒
          Seq(node(r)) ++ children.flatMap(inner) ++ children.flatMap(link(id, _))
        case Tree.Val(_, _) ⇒
          Seq.empty
      }
      trees.flatMap(inner)
    }

    val graph = StrictDigraph("g", statements: _*)
//    renderGraph(graph, System.out)
    dot2dot[Graph, File](graph, format = DotFormat.png)
  }
}
