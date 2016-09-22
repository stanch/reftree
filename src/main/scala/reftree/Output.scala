package reftree

import java.io.{File, StringWriter}
import java.nio.file.Path
import javax.xml.parsers.SAXParserFactory

import uk.co.turingatemyhamster.graphvizs.dsl.Graph
import uk.co.turingatemyhamster.graphvizs.exec._

import scala.sys.process.{Process, BasicIO}

object Output {
  private val saxParserFactory = SAXParserFactory.newInstance()
  saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
  private val XML = xml.XML.withSAXParser(saxParserFactory.newSAXParser())

  private def render(graph: Graph, output: Path, extraArgs: Seq[String]): Unit = {
    val args = Seq("-K", "dot", "-o", output.toString) ++ extraArgs
    val process = Process("dot", args)
    val io = BasicIO.standard(GraphInputHandler.handle(graph) _)
    (process run io).exitValue()
    ()
  }

  def renderPng(graph: Graph, output: Path, density: Int): Unit =
    render(graph, output, Seq("-T", "png", s"-Gdpi$density"))

  def renderSvg(graph: Graph, output: Path): Unit =
    render(graph, output, Seq("-T", "svg"))

  def renderSvg(graph: Graph): xml.Node = {
    val args = Seq("-K", "dot", "-T", "svg")
    val process = Process("dot", args)
    val output = new StringWriter
    val io = BasicIO.standard(GraphInputHandler.handle(graph) _).withOutput(BasicIO.processFully(output))
    (process run io).exitValue()
    XML.loadString(output.toString)
  }

  def renderPng(svg: xml.Node, output: Path, density: Int, silent: Boolean): Unit = {
    val svgFile = File.createTempFile("graph", ".svg")
    xml.XML.save(svgFile.getAbsolutePath, svg, "UTF-8", xmlDecl = true)
    val args = Seq(
      "-z",
      "-b", "white",
      "-d", density.toString,
      "-e", output.toString,
      svgFile.getAbsolutePath
    )
    val process = Process("inkscape", args)
    (if (silent) process run BasicIO(withIn = false, _ ⇒ (), None) else process.run()).exitValue()
    svgFile.delete()
  }

  def renderAnimatedGif(
    svgs: Seq[xml.Node],
    output: Path,
    options: Diagram.AnimationOptions,
    density: Int,
    silent: Boolean
  ): Unit = {
    val pngFiles = svgs map { svg ⇒
      val pngFile = File.createTempFile("graph", ".png")
      renderPng(svg, pngFile.toPath, density, silent)
      pngFile
    }
    val verboseArg = if (silent) Seq.empty else Seq("-verbose")
    val args = Seq(
      "-delay", options.delay.toString,
      "-loop", if (options.loop) "0" else "1"
    ) ++ verboseArg ++ pngFiles.map(_.getAbsolutePath) :+ output.toString
    val process = Process("convert", args)
    process.run().exitValue()
    pngFiles.foreach(_.delete())
  }
}
