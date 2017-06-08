package reftree.demo

import java.nio.file.Paths

import reftree.core._
import reftree.diagram.Diagram
import reftree.dot.Graph
import reftree.geometry.{Interpolation, Point}
import reftree.graph.Graphs
import reftree.render._
import reftree.svg._
import reftree.util.Optics
import zipper.Zipper

import scala.sys.process.Process

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

  def clear() = DotRenderer.render(
    Graph(true, true, Seq.empty), Paths.get("diagram.png"), RenderingOptions(), "png"
  )

  def graph[A: ToRefTree](value: A): Graph =
    Graphs.graph(RenderingOptions())(Diagram(value))

  def svg[A: ToRefTree](value: A): xml.Node = {
    import ScalaXmlSvgApi.svgUnzip
    Optics.collectFirst(ScalaXmlSvgApi.select("g.graph"))
      .composeOptional(ScalaXmlSvgApi.translation)
      .set(Point.zero)(xml.Utility.trim(AnimatedGifRenderer.renderSvg(graph(value))))
  }

  def renderFrames(
    start: xml.Node,
    end: xml.Node,
    interpolation: Interpolation[xml.Node],
    frames: Int
  ) = {
    AnimatedGifRenderer.renderAnimatedGif(
      start +: interpolation.sample(start, end, frames, inclusive = false) :+ end,
      Paths.get("diagram.gif"),
      RenderingOptions(density = 200),
      AnimationOptions(framesPerSecond = frames / 4)
    )
    Process(Seq("gifview", "diagram.gif")).!
    ()
  }

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
