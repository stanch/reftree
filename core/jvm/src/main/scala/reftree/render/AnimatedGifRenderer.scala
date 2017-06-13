package reftree.render

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, StringWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import javax.xml.parsers.SAXParserFactory

import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.nio.StreamingGifWriter
import org.apache.batik.transcoder.{TranscoderInput, SVGAbstractTranscoder, TranscoderOutput}
import org.apache.batik.transcoder.image.{ImageTranscoder, PNGTranscoder}
import reftree.dot.Graph

import scala.annotation.tailrec
import scala.sys.process.{Process, BasicIO}

object AnimatedGifRenderer {
  case class RenderingException(message: String) extends Exception(message)

  private lazy val saxParserFactory = {
    val instance = SAXParserFactory.newInstance()
    // This prevents the parser from going to the Internet every time!
    instance.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    instance
  }
  private lazy val XML = xml.XML.withSAXParser(saxParserFactory.newSAXParser())

  def renderSvg(graph: Graph): xml.Node = {
    val args = Seq("-K", "dot", "-T", "svg")
    val process = Process("dot", args)
    val output = new StringWriter
    val error = new StringWriter
    val io = BasicIO.standard { stream ⇒
      stream.write(graph.encode.getBytes(StandardCharsets.UTF_8))
      stream.close()
    }.withOutput(BasicIO.processFully(output)).withError(BasicIO.processFully(error))
    (process run io).exitValue()
    if (error.toString.nonEmpty) throw RenderingException(error.toString)
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

  @tailrec def spans[A](xs: List[A], acc: List[(A, Int)] = Nil): List[(A, Int)] =
    (xs, acc) match {
      case (Nil, _) ⇒ acc.reverse
      case (head :: tail, (last, c) :: prev) if head == last ⇒ spans(tail, (last, c + 1) :: prev)
      case (head :: tail, prev) ⇒ spans(tail, (head, 1) :: prev)
    }

  def renderAnimatedGif(
    svgs: Seq[xml.Node],
    output: Path,
    renderingOptions: RenderingOptions,
    animationOptions: AnimationOptions
  ): Unit = {
    val writer = StreamingGifWriter(animationOptions.delay, animationOptions.loop)
    val stream = writer.prepareStream(output, BufferedImage.TYPE_INT_ARGB)
    spans(svgs.toList) foreach {
      case (svg, count) ⇒
        val image = renderImage(svg, renderingOptions)
        Seq.fill(count)(stream.writeFrame(image))
    }
    stream.finish()
  }

  def renderAnimatedGif(
    graphs: Seq[Graph],
    svgPreprocessing: Seq[xml.Node] ⇒ Seq[xml.Node],
    output: Path,
    renderingOptions: RenderingOptions,
    animationOptions: AnimationOptions
  ): Unit =
    renderAnimatedGif(svgPreprocessing(graphs.map(renderSvg)), output, renderingOptions, animationOptions)
}
