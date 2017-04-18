package reftree.demo

import reftree.core._
import reftree.diagram.Diagram
import reftree.geometry.Point
import reftree.graph.{Graph, Graphs}
import reftree.render.{AnimatedGifRenderer, RenderingOptions, Renderer}
import reftree.svg._
import reftree.util.Optics
import zipper.Zipper

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
    Optics.collectFirst(sel"g.graph")
      .composeOptional(SvgOptics.translation)
      .set(Point.zero)(xml.Utility.trim(AnimatedGifRenderer.renderSvg(graph(value))))

  private def tapRender[B: ToRefTree](value: B) = { render(value); value }

  def zipperControl[A](zipper: Zipper[A])(implicit toRefTree: ToRefTree[Zipper[A]]): Unit = {
    Iterator
      .continually(Console.in.read())
      .takeWhile(_ != 'q')
      .filter(Set('w', 'a', 's', 'd'))
      .foldLeft(tapRender(zipper)) {
        case (z, 'w') ⇒ tapRender(z.tryMoveUp.orStay)
        case (z, 'a') ⇒ tapRender(z.tryMoveLeft.orStay)
        case (z, 's') ⇒ tapRender(z.tryMoveDownLeft.orStay)
        case (z, 'd') ⇒ tapRender(z.tryMoveRight.orStay)
        case (z, _) ⇒ z
      }
  }
}
