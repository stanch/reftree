package reftree

import java.nio.file.{Paths, Path}

object Diagram {
  case class Options(
    density: Int = 300,
    verticalSpacing: Double = 0.8,
    palette: Seq[String] = Array("dodgerblue4", "forestgreen", "coral3"),
    highlightColor: String = "bisque",
    labels: Boolean = true,
    commonNodesBelongToLastTree: Boolean = false
  )

  case class AnimationOptions(
    delay: Int = 100,
    loop: Boolean = true,
    onionSkin: Int = 1,
    anchoring: Boolean = true,
    diffAccent: Boolean = false,
    density: Int = 100,
    verticalSpacing: Double = 0.8,
    color: String = "dodgerblue4",
    accentColor: String = "forestgreen",
    highlightColor: String = "bisque",
    silent: Boolean = true
  ) {
    def toOptions = {
      val palette = if (onionSkin == 0) Seq(color) else {
        (50 to 80 by (30 / onionSkin)).take(onionSkin).map(i ⇒ s"gray$i").reverse :+ color
      }
      Options(
        density, verticalSpacing, palette, highlightColor,
        labels = false, commonNodesBelongToLastTree = true
      )
    }
  }
}

case class Diagram(
  defaultOptions: Diagram.Options = Diagram.Options(),
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

  private def processFrames[A: ToRefTree](options: AnimationOptions)(data: Seq[A]): Seq[xml.Node] = {
    require(data.length > 1, "at least two frames should be provided")
    val trees = data.map(d ⇒ LabeledRefTree(d.toString, d.refTree))
    val frames = Graphs.graphFrames(options)(trees)
    val svgs = frames.map(Output.renderSvg)
    val ids = trees.map(_.tree) collect { case RefTree.Ref(_, id, _, _) ⇒ id }
    Svgs.adjust(svgs, ids, options.anchoring)
  }

  def renderSequence[A: ToRefTree](
    baseName: String,
    tweakOptions: AnimationOptions ⇒ AnimationOptions = identity,
    directory: Path = defaultDirectory
  )(data: Seq[A]): Unit = {
    val tweakedOptions = tweakOptions(defaultAnimationOptions)
    val adjustedSvgs = processFrames(tweakOptions(defaultAnimationOptions))(data)
    val indexWidth = adjustedSvgs.length.toString.length
    def padding(index: Int) = index.toString.reverse.padTo(indexWidth, '0').reverse
    adjustedSvgs.zipWithIndex foreach { case (svg, i) ⇒
      Output.renderPng(svg, directory.resolve(s"$baseName-${padding(i + 1)}.png"), tweakedOptions)
    }
  }

  def renderAnimation[A: ToRefTree](
    name: String,
    tweakOptions: AnimationOptions ⇒ AnimationOptions = identity,
    directory: Path = defaultDirectory
  )(data: Seq[A]): Unit = {
    val tweakedOptions = tweakOptions(defaultAnimationOptions)
    val adjustedSvgs = processFrames(tweakedOptions)(data)
    Output.renderAnimatedGif(adjustedSvgs, directory.resolve(s"$name.gif"), tweakedOptions)
  }
}
