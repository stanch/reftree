package reftree.demo

import java.io.{FileOutputStream, File}
import java.nio.file.{Files, Paths}

import reftree.core._
import reftree.diagram.Diagram
import reftree.dot.Graph
import reftree.geometry.{Interpolation, Point}
import reftree.graph.Graphs
import reftree.render._
import reftree.svg._
import reftree.svg.animation.Frame
import reftree.util.Optics
import zipper.Zipper

import scala.sys.process.Process

object Shortcuts {
  val renderer = Renderer()

  private var shownSlides = Set.empty[String]

  def render[A: ToRefTree](value: sourcecode.Text[A]) = {
    val id = value.source.filter(_.isLetter)
    val slide = Paths.get("slides", s"$id.png")
    if (!shownSlides(id) &&
      slide.toFile.exists()) {
      val stream = new FileOutputStream("diagram.png")
      Files.copy(slide, stream)
      stream.close()
      shownSlides += id
    } else renderer.render("diagram", Diagram.sourceCodeCaption(value))
  }

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
    Graph(true, true, None, Seq.empty), Paths.get("diagram.png"), RenderingOptions(), "png"
  )

  def generic[A](value: A)(implicit generic: shapeless.LabelledGeneric[A]) =
    generic.to(value)

  def refTree[A: ToRefTree](value: A): RefTree =
    value.refTree

  def graph[A: ToRefTree](value: A): Graph =
    Graphs.graph(RenderingOptions())(Diagram(value))

  def svg[A: ToRefTree](value: A): xml.Node = {
    import XmlSvgApi.svgUnzip
    Optics.collectFirst(XmlSvgApi.select("g.graph"))
      .composeOptional(XmlSvgApi.translation)
      .set(Point.zero)(xml.Utility.trim(AnimatedGifRenderer.renderSvg(graph(value))))
  }

  def renderFrames(
    start: xml.Node,
    end: xml.Node,
    interpolation: Interpolation[xml.Node],
    frames: Int
  ) = {
    val output = File.createTempFile("animation", ".gif")
    output.deleteOnExit()
    AnimatedGifRenderer.renderFrames(
      (start +: interpolation.sample(start, end, frames, inclusive = false) :+ end)
        .map(Frame(_)),
      output.toPath,
      RenderingOptions(density = 200),
      AnimationOptions(framesPerSecond = frames / 4)
    )
    Process(Seq("gifview", "--animate", output.getAbsolutePath)).run()
    ()
  }

  private def tapRender[B: ToRefTree](value: B) = { render(value); value }

  def zipperControl[A](zipper: Zipper[A])(implicit toRefTree: ToRefTree[Zipper[A]]): Unit = {
    Iterator
      .continually(Console.in.read())
      .map(_.toChar)
      .takeWhile(_ != 'q')
      .filter(Set('w', 'a', 's', 'd'))
      .foldLeft(tapRender(zipper)) {
        case (z, 'w') => tapRender(z.tryMoveUp.orStay)
        case (z, 'a') => tapRender(z.tryMoveLeft.orStay)
        case (z, 's') => tapRender(z.tryMoveDownLeft.orStay)
        case (z, 'd') => tapRender(z.tryMoveRight.orStay)
        case (z, _) => z
      }
  }
}
