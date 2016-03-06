package diapers

import com.github.mdr.ascii.graph.Graph
import com.github.mdr.ascii.layout._

import ToRefTree._

object AsciiPlotter {
  private case class Vertex(id: String, text: String) {
    override def toString = text
  }

  private def vertex(ref: RefTree.Ref) = {
    val cells = ref.children.collect {
      case RefTree.Ref(name, _, _) ⇒ s"<$name>"
      case RefTree.Val(v: Int, Some(RefTree.Val.Bin)) ⇒ v.toBinaryString
      case RefTree.Val(v, _) ⇒ v.toString
    }.mkString(" (", " | ", ")")
    Vertex(ref.id, ref.name + cells)
  }

  def graph[A <: AnyRef: ToRefTree](objects: A*) = {
    val vertices = {
      def inner(tree: RefTree): Set[RefTree.Ref] = tree match {
        case r: RefTree.Ref ⇒ r.children.flatMap(inner).toSet + r
        case _ ⇒ Set.empty
      }
      objects.map(_.refTree).flatMap(inner).toSet.map(vertex)
    }

    val edges = {
      def inner(tree: RefTree): Seq[(RefTree.Ref, RefTree.Ref)] = tree match {
        case r: RefTree.Ref ⇒ r.children
          .collect { case rr: RefTree.Ref ⇒ rr }
          .flatMap(rr ⇒ inner(rr) :+ (r → rr))
        case _ ⇒ Seq.empty
      }
      objects.map(_.refTree).flatMap(inner).map { case (x, y) ⇒ vertex(x) → vertex(y) }.toList
    }

    GraphLayout.renderGraph(Graph(vertices, edges))
  }

  def plot[A <: AnyRef: ToRefTree](objects: A*) = println(graph(objects: _*))
}
