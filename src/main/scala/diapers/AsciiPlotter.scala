package diapers

import com.github.mdr.ascii.graph.Graph
import com.github.mdr.ascii.layout._

object AsciiPlotter {
  case class Vertex(id: String, text: String) {
    override def toString = text
  }

  def plot(data: Data*) = {
    def vertex(ref: Data.Ref) = {
      val cells = ref.data.collect {
        case Data.Ref(name, _, _) ⇒ s"<$name>"
        case Data.Val(v) ⇒ v.toString
      }.mkString(" (", " | ", ")")
      Vertex(ref.id, ref.name + cells)
    }

    val vertices = {
      def inner(x: Data): Set[Data.Ref] = x match {
        case r: Data.Ref ⇒ r.data.flatMap(inner).toSet + r
        case _ ⇒ Set.empty
      }
      data.flatMap(inner).toSet.map(vertex)
    }

    val edges = {
      def inner(x: Data): Seq[(Data.Ref, Data.Ref)] = x match {
        case r: Data.Ref ⇒ r.data
          .collect { case rr: Data.Ref ⇒ rr }
          .flatMap(rr ⇒ inner(rr) :+ (r → rr))
        case _ ⇒ Seq.empty
      }
      data.flatMap(inner).map { case (x, y) ⇒ vertex(x) → vertex(y) }.toList
    }

    GraphLayout.renderGraph(Graph(vertices, edges))
  }
}
