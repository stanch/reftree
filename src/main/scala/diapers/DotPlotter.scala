package diapers

import java.nio.file.{Paths, Path}

import uk.co.turingatemyhamster.graphvizs.dsl._
import uk.co.turingatemyhamster.graphvizs.exec._

import diapers.ToRefTree._

import scala.sys.process.{Process, ProcessIO}

case class DotPlotter(output: Path = Paths.get("graph.png"), ranksep: Double = 0.8) {
  private def node(ref: RefTree.Ref): NodeStatement = {
    val cells = ref.children.map(cell)
    val label = (s"<n>${ref.name}" +: cells).mkString("|")
    ref.id :| ("label" := label)
  }

  private def cell(tree: RefTree): String = tree match {
    case RefTree.Val(value: Int, Some(RefTree.Val.Bin)) ⇒ value.toBinaryString
    case RefTree.Val(value, _) ⇒ value.toString.replace(" ", "_")
    case RefTree.Ref(_, id, _) ⇒ s"<$id>&middot;"
  }

  private def link(id: String, tree: RefTree): Option[EdgeStatement] = tree match {
    case RefTree.Val(_, _) ⇒ None
    case RefTree.Ref(_, linkId, _) ⇒ Some(
      NodeId(id, Some(Port(Some(linkId), Some(CompassPt.S)))) -->
      NodeId(linkId, Some(Port(Some("n"), Some(CompassPt.N))))
    )
  }

  def plot[A <: AnyRef: ToRefTree](objects: A*) = {
    val graphAttrs = "graph" :| ("ranksep" := ranksep)
    val nodeAttrs = "node" :| ("shape" := "Mrecord")
    val statements: Seq[Statement] = Seq(graphAttrs, nodeAttrs) ++ {
      def inner(tree: RefTree): Seq[Statement] = tree match {
        case r @ RefTree.Ref(_, id, children) ⇒
          Seq(node(r)) ++ children.flatMap(inner) ++ children.flatMap(link(id, _))
        case RefTree.Val(_, _) ⇒
          Seq.empty
      }
      objects.map(_.refTree).flatMap(inner)
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
