package reftree

import java.awt.image.BufferedImage
import java.io._
import java.nio.file.Path
import javax.xml.parsers.SAXParserFactory

import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.GifSequenceWriter
import org.apache.batik.transcoder.{SVGAbstractTranscoder, TranscoderInput, TranscoderOutput}
import org.apache.batik.transcoder.image.{ImageTranscoder, PNGTranscoder}
import uk.co.turingatemyhamster.graphvizs.dsl.Graph
import uk.co.turingatemyhamster.graphvizs.exec._

import scala.sys.process.{BasicIO, Process}

object Output {
  // Normal rendering

  def renderPng(graph: Graph, output: Path, options: Diagram.Options): Unit = {
    val args = Seq(
      "-K", "dot",
      "-T", "png",
      s"-Gdpi=${options.density}",
      "-o", output.toString
    )
    val process = Process("dot", args)
    val io = BasicIO.standard(GraphInputHandler.handle(graph) _)
    (process run io).exitValue()
    ()
  }

  // SVG-based pipeline for animations

  private lazy val saxParserFactory = {
    val instance = SAXParserFactory.newInstance()
    instance.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    instance
  }
  private lazy val XML = xml.XML.withSAXParser(saxParserFactory.newSAXParser())

  def renderSvg(graph: Graph): xml.Node = {
    val args = Seq("-K", "dot", "-T", "svg")
    val process = Process("dot", args)
    val output = new StringWriter
    val io = BasicIO.standard(GraphInputHandler.handle(graph) _).withOutput(BasicIO.processFully(output))
    (process run io).exitValue()
    XML.loadString(output.toString)
  }

  def renderImage(svg: xml.Node, options: Diagram.AnimationOptions): Image = {
    var image: BufferedImage = null
    val transcoder = new PNGTranscoder {
      override def writeImage(img: BufferedImage, output: TranscoderOutput): Unit = image = img
      addTranscodingHint(
        ImageTranscoder.KEY_BACKGROUND_COLOR,
        java.awt.Color.white
      )
      addTranscodingHint(
        SVGAbstractTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER,
        new java.lang.Float(25.4 / options.density)
      )
    }
    val inputStream = new ByteArrayInputStream(svg.toString().getBytes("UTF-8"))
    val transcoderInput = new TranscoderInput(inputStream)
    transcoder.transcode(transcoderInput, null)
    inputStream.close()
    Image.fromAwt(image)
  }

  def renderAnimatedGif(svgs: Seq[xml.Node], output: Path, options: Diagram.AnimationOptions): Unit = {
    val delay = options.delay / Math.max(options.interpolationFrames * 2, 1)
    val writer = GifSequenceWriter(delay, options.loop)
    writer.output(svgs.par.map(renderImage(_, options)).to[Seq], output)
  }
}
