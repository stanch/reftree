package diapers

import com.github.mdr.ascii.graph.Graph
import com.github.mdr.ascii.layout._

object AsciiPlotter {
  case class Vertex(id: String, text: String) {
    override def toString = text
  }

  def plot(trees: Tree*) = {
    def vertex(ref: Tree.Ref) = {
      val cells = ref.children.collect {
        case Tree.Ref(name, _, _) ⇒ s"<$name>"
        case Tree.Val(v) ⇒ v.toString
      }.mkString(" (", " | ", ")")
      Vertex(ref.id, ref.name + cells)
    }

    val vertices = {
      def inner(tree: Tree): Set[Tree.Ref] = tree match {
        case r: Tree.Ref ⇒ r.children.flatMap(inner).toSet + r
        case _ ⇒ Set.empty
      }
      trees.flatMap(inner).toSet.map(vertex)
    }

    val edges = {
      def inner(tree: Tree): Seq[(Tree.Ref, Tree.Ref)] = tree match {
        case r: Tree.Ref ⇒ r.children
          .collect { case rr: Tree.Ref ⇒ rr }
          .flatMap(rr ⇒ inner(rr) :+ (r → rr))
        case _ ⇒ Seq.empty
      }
      trees.flatMap(inner).map { case (x, y) ⇒ vertex(x) → vertex(y) }.toList
    }

    GraphLayout.renderGraph(Graph(vertices, edges))
  }
}
