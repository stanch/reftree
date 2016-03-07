package reftree

import com.github.mdr.ascii.graph.Graph
import com.github.mdr.ascii.layout._

object AsciiPlotter {
  private case class Vertex(id: String, text: String) {
    override def toString = text
  }

  private def vertex(ref: RefTree.Ref) = {
    val cells = ref.children.collect {
      case RefTree.Ref(name, _, _) ⇒ s"<$name>"
      case RefTree.Val(v: Int, Some(RefTree.Val.Bin)) ⇒ v.toBinaryString
      case RefTree.Val(v, _) ⇒ v.toString
      case RefTree.Null ⇒ "∅"
      case RefTree.Undefined ⇒ "░"
    }.mkString(" (", " | ", ")")
    Vertex(ref.id, ref.name + cells)
  }

  private def label(tree: LabeledRefTree) = tree match {
    case LabeledRefTree(label, ref: RefTree.Ref) ⇒ Some(Vertex(s"${ref.id}-label", label))
    case _ ⇒ None
  }

  // TODO: this is somewhat ugly and inefficient
  def graph(trees: LabeledRefTree*) = {
    val vertices = {
      def inner(tree: RefTree): Set[RefTree.Ref] = tree match {
        case r: RefTree.Ref ⇒ r.children.flatMap(inner).toSet + r
        case _ ⇒ Set.empty
      }
      trees.map(_.tree).flatMap(inner).toSet.map(vertex) ++ trees.flatMap(label)
    }

    val edges = {
      def inner(tree: RefTree): Seq[(RefTree.Ref, RefTree.Ref)] = tree match {
        case r: RefTree.Ref ⇒ r.children
          .collect { case rr: RefTree.Ref ⇒ rr }
          .flatMap(rr ⇒ inner(rr) :+ (r → rr))
        case _ ⇒ Seq.empty
      }
      trees.map(_.tree).flatMap(inner).map { case (x, y) ⇒ vertex(x) → vertex(y) }.toList ++
      trees.collect { case l @ LabeledRefTree(_, r: RefTree.Ref) ⇒ label(l).get → vertex(r) }.toList
    }

    GraphLayout.renderGraph(Graph(vertices, edges))
  }

  def plot(trees: LabeledRefTree*) = println(graph(trees: _*))
}
