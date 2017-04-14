package reftree.demo

import reftree.core._
import reftree.diagram.Diagram
import reftree.graph.{Graph, Graphs}
import reftree.render.{AnimatedGifRenderer, RenderingOptions, Renderer}

object Shortcuts {
  val renderer = Renderer()

  def render[A: ToRefTree](value: sourcecode.Text[A]) =
    renderer.render("diagram", Diagram.sourceCodeCaption(value))

  def render[A: ToRefTree, B: ToRefTree](
    value1: sourcecode.Text[A],
    value2: sourcecode.Text[B]
  ) = renderer.render(
    "diagram",
    Diagram.sourceCodeCaption(value1) + Diagram.sourceCodeCaption(value2)
  )

  def render[A: ToRefTree, B: ToRefTree, C: ToRefTree](
    value1: sourcecode.Text[A],
    value2: sourcecode.Text[B],
    value3: sourcecode.Text[C]
  ) = renderer.render(
    "diagram",
    Diagram.sourceCodeCaption(value1) + Diagram.sourceCodeCaption(value2) + Diagram.sourceCodeCaption(value3)
  )

  def graph[A: ToRefTree](value: A): Graph =
    Graphs.graph(RenderingOptions())(Diagram(value))

  def svg[A: ToRefTree](value: A): xml.Node =
    AnimatedGifRenderer.renderSvg(graph(value))
}
