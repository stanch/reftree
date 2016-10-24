package reftree.render

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, StringWriter}
import java.nio.file.Path
import javax.xml.parsers.SAXParserFactory

import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.GifSequenceWriter
import org.apache.batik.transcoder.{TranscoderInput, SVGAbstractTranscoder, TranscoderOutput}
import org.apache.batik.transcoder.image.{ImageTranscoder, PNGTranscoder}
import uk.co.turingatemyhamster.graphvizs.dsl.Graph
import uk.co.turingatemyhamster.graphvizs.exec._

import scala.sys.process.{Process, BasicIO}

object AnimatedGifRenderer {
  private lazy val saxParserFactory = {
    val instance = SAXParserFactory.newInstance()
    instance.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    instance
  }
  private lazy val XML = xml.XML.withSAXParser(saxParserFactory.newSAXParser())

  private def renderSvg(graph: Graph): xml.Node = {
    val args = Seq("-K", "dot", "-T", "svg")
    val process = Process("dot", args)
    val output = new StringWriter
    val io = BasicIO.standard(GraphInputHandler.handle(graph) _).withOutput(BasicIO.processFully(output))
    (process run io).exitValue()
    XML.loadString(output.toString)
  }

  private def renderImage(svg: xml.Node, options: RenderingOptions): Image = {
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

  def renderAnimatedGif(
    graphs: Seq[Graph],
    svgPreprocessing: Seq[xml.Node] ⇒ Seq[xml.Node],
    output: Path,
    renderingOptions: RenderingOptions,
    animationOptions: AnimationOptions
  ): Unit = {
    val svgs = svgPreprocessing(graphs.map(renderSvg))
    val images = svgs.par.map(renderImage(_, renderingOptions)).to[Seq]
    val duplicated = Seq.fill(animationOptions.keyFrames)(images.head) ++
      images.tail.grouped(animationOptions.interpolationFrames + 1).flatMap {
        case init :+ last ⇒ init ++ Seq.fill(animationOptions.keyFrames)(last)
      }
    // TODO: use the streaming writer
    val writer = GifSequenceWriter(animationOptions.delay, animationOptions.loop)
    writer.output(duplicated, output)
  }
}
