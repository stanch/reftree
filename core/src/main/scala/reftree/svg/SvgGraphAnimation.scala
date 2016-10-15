package reftree.svg

import reftree.Diagram.AnimationOptions
import reftree.geometry._

import scala.util.Try

object SvgGraphAnimation {
  private def fixTextColor(svg: xml.Node) = SvgGraphLens.nodes.modify { nodes ⇒
    // Graphviz does not set the fill-opacity attribute on text
    nodes map { case (id, node) ⇒ id → SvgGraphLens.color.modify(identity)(node) }
  }(svg)

  private def align(prev: xml.Node, next: xml.Node, prevAnchorId: String, nextAnchorId: String) = Try {
    val prevPosition =
      SvgGraphLens.node(prevAnchorId) composeLens
        SvgGraphLens.nodePosition get prev
    val nextPosition =
      SvgGraphLens.node(nextAnchorId) composeLens
        SvgGraphLens.nodePosition get next
    val translation = prevPosition - nextPosition
    val withBox = SvgLens.viewBox.modify(_ + translation)(next)
    (SvgGraphLens.graph composeLens SvgLens.translation).modify(_ + translation)(withBox)
  }.toOption

  private def alignPairwise(svgs: Seq[xml.Node], anchorIds: Seq[String], options: AnimationOptions) =
    (svgs.tail zip anchorIds.sliding(2).toSeq).foldLeft(Vector(svgs.head)) {
      case (acc :+ prev, (next, Seq(prevAnchorId, nextAnchorId))) ⇒
        val anchoringAttempt = if (!options.anchoring) None else {
          align(prev, next, prevAnchorId, prevAnchorId)
        }
        lazy val default = align(prev, next, prevAnchorId, nextAnchorId).get
        acc :+ prev :+ (anchoringAttempt getOrElse default)
    }

  private val interpolation: Interpolation[xml.Node] = {
    val fadeOut = Interpolation.double.withRight(0.0).lens(SvgLens.opacity).timespan(0, 1/3.0)
    val fadeIn = Interpolation.double.withLeft(0.0).lens(SvgLens.opacity).timespan(2/3.0, 1)

    val opacityAndColor = Interpolation.combineLeft(
      Interpolation.double.lensLeft(SvgLens.opacity).timespan(1/3.0, 2/3.0),
      Color.RGBA.interpolation.lensLeft(SvgGraphLens.color).timespan(1/3.0, 1)
    )

    val nodePosition = Point.interpolation.lensLeft(SvgGraphLens.nodePosition)
      .timespan(1/3.0, 2/3.0)

    val nodeHighlight = Interpolation.option(
      Color.RGBA.interpolation.withRight(_.copy(a = 0.0)).timespan(0, 1/2.0),
      Color.RGBA.interpolation.withLeft(_.copy(a = 0.0)).timespan(1/2.0, 1),
      Color.RGBA.interpolation
    ).lensLeft(SvgGraphLens.nodeHighlight)

    val edgePosition = Interpolation.combineLeft(
      Polyline.interpolation.lensLeft(SvgGraphLens.edgeArrow),
      Path.interpolation.lensLeft(SvgGraphLens.edgePath)
    ).timespan(1/3.0, 2/3.0)

    val node = Interpolation.option(
      fadeOut, fadeIn, Interpolation.combineLeft(opacityAndColor, nodeHighlight, nodePosition)
    )

    val edge = Interpolation.option(
      fadeOut, fadeIn, Interpolation.combineLeft(opacityAndColor, edgePosition)
    )

    Interpolation.combineLeft(
      Interpolation.map(node).lensLeft(SvgGraphLens.nodes),
      Interpolation.map(edge).lensLeft(SvgGraphLens.edges)
    )
  }

  private def interpolatePairwise(svgs: Seq[xml.Node], options: AnimationOptions) =
    Seq.fill(options.interpolationFrames + 1)(svgs.head) ++
      svgs.sliding(2).toSeq.flatMap {
        case Seq(prev, next) ⇒
          interpolation.sample(prev, next, options.interpolationFrames, inclusive = false) ++
            Seq.fill(options.interpolationFrames + 1)(next)
      }

  def animate(svgs: Seq[xml.Node], anchorIds: Seq[String], options: AnimationOptions) = {
    val colored = svgs.map(fixTextColor)
    val aligned = alignPairwise(colored, anchorIds, options)
    val maxViewBox = Rectangle.union(aligned.map(SvgLens.viewBox.get))
    val resized = aligned.map(SvgLens.viewBox.set(maxViewBox))
    interpolatePairwise(resized, options)
  }
}
