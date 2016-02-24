package diapers

import java.io.File

import uk.co.turingatemyhamster.graphvizs.dsl._
import uk.co.turingatemyhamster.graphvizs.exec._

object DotPlotter {
  def node(ref: Data.Ref): NodeStatement = {
    val cells = ref.data.map(cell)
    val label = (s"<n>${ref.name}" +: cells).mkString("|")
    ref.id :| ("label" := label)
  }

  def cell(data: Data): String = data match {
    case Data.Val(value) ⇒ value.toString.replace(" ", "_")
    case Data.Ref(_, id, _) ⇒ s"<$id>&middot;"
  }

  def link(id: String, data: Data): Option[EdgeStatement] = data match {
    case Data.Val(_) ⇒ None
    case Data.Ref(_, linkId, _) ⇒ Some(
      NodeId(id, Some(Port(Some(linkId), Some(CompassPt.S)))) -->
      NodeId(linkId, Some(Port(Some("n"), Some(CompassPt.N))))
    )
  }

  def plot(data: Data*) = {
    val record = "node" :| ("shape" := "Mrecord")
    val statements: Seq[Statement] = record +: {
      def inner(x: Data): Seq[Statement] = x match {
        case r @ Data.Ref(_, id, children) ⇒
          Seq(node(r)) ++ children.flatMap(inner) ++ children.flatMap(link(id, _))
        case Data.Val(_) ⇒
          Seq.empty
      }
      data.flatMap(inner)
    }

    val graph = StrictDigraph("g", statements: _*)
//    renderGraph(graph, System.out)
    dot2dot[Graph, File](graph, format = DotFormat.png)
  }
}
