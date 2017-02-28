package reftree.render

import java.nio.file.Path

import uk.co.turingatemyhamster.graphvizs.dsl.Graph
import uk.co.turingatemyhamster.graphvizs.exec._

import scala.sys.process.{Process, BasicIO}

object DotRenderer {
  def render(graph: Graph, output: Path,
    options: RenderingOptions, format: String
  ): Unit = {
    val args = Seq(
      "-K", "dot",
      "-T", format,
      s"-Gdpi=${options.density}",
      "-o", output.toString
    )
    val process = Process("dot", args)
    val io = BasicIO.standard(GraphInputHandler.handle(graph) _)
    (process run io).exitValue()
    ()
  }
}
