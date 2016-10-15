package reftree

import java.nio.file.{Paths, Path}

import reftree.geometry.Interpolation
import reftree.svg.SvgGraphAnimation

import scala.concurrent.duration._

object Diagram {
  sealed trait RenderingOptions {
    def density: Int
    def verticalSpacing: Double
    def palette: Seq[String]
    def highlightColor: String
    def labels: Boolean
    def commonNodesBelongToLastTree: Boolean
  }

  sealed trait SequenceRenderingOptions extends RenderingOptions {
    def interpolationFrames: Int
    def onionSkinLayers: Int
    def anchoring: Boolean
    def diffAccent: Boolean

    def color: String
    def onionSkinBaseColor: String
    def accentColor: String

    final def palette = if (onionSkinLayers == 0) Seq(color) else {
      Interpolation.double.sample(0, 255, onionSkinLayers, inclusive = false)
        .map(o ⇒ f"$onionSkinBaseColor${o.toInt}%02x") :+ color
    }

    final def labels = false
    final def commonNodesBelongToLastTree = true
  }

  case class Options(
    density: Int = 300,
    verticalSpacing: Double = 0.8,
    palette: Seq[String] = Array("dodgerblue4", "forestgreen", "coral3"),
    highlightColor: String = "bisque",
    labels: Boolean = true,
    commonNodesBelongToLastTree: Boolean = false
  ) extends RenderingOptions

  case class SequenceOptions(
    interpolationFrames: Int = 3,
    onionSkinLayers: Int = 0,
    anchoring: Boolean = true,
    diffAccent: Boolean = false,
    density: Int = 100,
    verticalSpacing: Double = 0.8,
    color: String = "dodgerblue4",
    onionSkinBaseColor: String = "#898988",
    accentColor: String = "#228B22",
    highlightColor: String = "#ffe4c4"
  ) extends SequenceRenderingOptions

  case class AnimationOptions(
    keyFrameDuration: FiniteDuration = 2.seconds,
    interpolationDuration: FiniteDuration = 1.seconds,
    framesPerSecond: Int = 9,
    loop: Boolean = true,
    onionSkinLayers: Int = 0,
    anchoring: Boolean = true,
    diffAccent: Boolean = false,
    density: Int = 100,
    verticalSpacing: Double = 0.8,
    color: String = "dodgerblue4",
    onionSkinBaseColor: String = "#898988",
    accentColor: String = "#228B22",
    highlightColor: String = "#ffe4c4"
  ) extends SequenceRenderingOptions {
    def interpolationFrames = Math.round(interpolationDuration.toMillis * framesPerSecond / 1000.0f)

    def keyFrames = if (interpolationFrames == 0) 1 else {
      Math.round(keyFrameDuration.toMillis * framesPerSecond / 1000.0f)
    }

    def delay = if (interpolationFrames == 0) keyFrameDuration else 1.second / framesPerSecond
  }
}

case class Diagram(
  defaultOptions: Diagram.Options = Diagram.Options(),
  defaultSequenceOptions: Diagram.SequenceOptions = Diagram.SequenceOptions(),
  defaultAnimationOptions: Diagram.AnimationOptions = Diagram.AnimationOptions(),
  defaultName: String = "diagram",
  defaultDirectory: Path = Paths.get(".")
) {
  import Diagram._

  def renderDefault(trees: LabeledRefTree*): Unit =
    render(defaultName)(trees: _*)

  def renderDefault(tweakOptions: Options ⇒ Options)(trees: LabeledRefTree*): Unit =
    render(defaultName, tweakOptions)(trees: _*)

  def render(
    name: String,
    tweakOptions: Options ⇒ Options = identity,
    directory: Path = defaultDirectory
  )(trees: LabeledRefTree*): Unit = {
    val tweakedOptions = tweakOptions(defaultOptions)
    val graph = Graphs.graph(tweakedOptions)(trees)
    Output.renderPng(graph, directory.resolve(s"$name.png"), tweakedOptions)
  }

  private def processFrames[A: ToRefTree](options: SequenceRenderingOptions)(data: Seq[A]): Seq[xml.Node] = {
    require(data.length > 1, "at least two frames should be provided")
    val trees = data.map(d ⇒ LabeledRefTree(d.toString, d.refTree))
    val frames = Graphs.graphFrames(options)(trees)
    val svgs = frames.map(Output.renderSvg)
    val ids = trees.map(_.tree) collect { case RefTree.Ref(_, id, _, _) ⇒ id }
    SvgGraphAnimation.animate(svgs, ids, options)
  }

  def renderSequence[A: ToRefTree](
    baseName: String,
    tweakOptions: SequenceOptions ⇒ SequenceOptions = identity,
    directory: Path = defaultDirectory
  )(data: Seq[A]): Unit = {
    val tweakedOptions = tweakOptions(defaultSequenceOptions)
    val processedFrames = processFrames(tweakOptions(defaultSequenceOptions))(data)
    val indexWidth = processedFrames.length.toString.length
    def padding(index: Int) = index.toString.reverse.padTo(indexWidth, '0').reverse
    processedFrames.zipWithIndex.par foreach { case (svg, i) ⇒
      Output.renderImage(svg, tweakedOptions).output(directory.resolve(s"$baseName-${padding(i + 1)}.png"))
    }
  }

  def renderAnimation[A: ToRefTree](
    name: String,
    tweakOptions: AnimationOptions ⇒ AnimationOptions = identity,
    directory: Path = defaultDirectory
  )(data: Seq[A]): Unit = {
    val tweakedOptions = tweakOptions(defaultAnimationOptions)
    val processedFrames = processFrames(tweakedOptions)(data)
    Output.renderAnimatedGif(processedFrames, directory.resolve(s"$name.gif"), tweakedOptions)
  }
}
