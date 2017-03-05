package reftree.render

import java.io.StringWriter
import java.nio.file.Path

import uk.co.turingatemyhamster.graphvizs.dsl.Graph
import uk.co.turingatemyhamster.graphvizs.exec._

import scala.sys.process.{Process, BasicIO}

object DotRenderer {
  case class RenderingException(message: String) extends Exception(message)

  def render(
    graph: Graph, output: Path, options: RenderingOptions, format: String
  ): Unit = {
    val args = Seq(
      "-K", "dot",
      "-T", format,
      s"-Gdpi=${options.density}",
      "-o", output.toString
    )
    val process = Process("dot", args)
    val error = new StringWriter
    val io = BasicIO.standard(GraphInputHandler.handle(graph) _)
      .withError(BasicIO.processFully(error))
    (process run io).exitValue()
    if (error.toString.nonEmpty) throw RenderingException(error.toString)
    ()
  }
}
