package reftree.render

import reftree.dot.Graph

import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Path

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
    val io = BasicIO.standard { stream ⇒
      stream.write(graph.toString.getBytes(StandardCharsets.UTF_8))
      stream.close()
    }.withError(BasicIO.processFully(error))
    (process run io).exitValue()
    if (error.toString.nonEmpty) throw RenderingException(error.toString)
    ()
  }
}
