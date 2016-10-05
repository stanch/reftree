package reftree.svg

import reftree.Diagram.AnimationOptions
import reftree.geometry._

import scala.util.Try

object SvgGraphAnimation {
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
    val opacity = Interpolation.double.lensBefore(SvgLens.opacity)
    val fadeIn = opacity.mapTime(_ * 3 - 2).withBefore(SvgLens.opacity.set(0.0))
    val fadeOut = opacity.mapTime(_ * 3).withAfter(SvgLens.opacity.set(0.0))

    val nodeOption = Interpolation.option(
      fadeOut, fadeIn, Interpolation.foldLeftBefore(
        opacity,
        Point.interpolation.lensBefore(SvgGraphLens.nodePosition)
      ).mapTime(_ * 3 - 1)
    )

    val edgeOption = Interpolation.option(
      fadeOut, fadeIn, Interpolation.foldLeftBefore(
        opacity,
        Polyline.interpolation.lensBefore(SvgGraphLens.edgeArrow),
        Path.interpolation.lensBefore(SvgGraphLens.edgePath)
      ).mapTime(_ * 3 - 1)
    )

    Interpolation.foldLeftBefore(
      Interpolation.map(nodeOption).lensBefore(SvgGraphLens.nodes),
      Interpolation.map(edgeOption).lensBefore(SvgGraphLens.edges)
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
    val aligned = alignPairwise(svgs, anchorIds, options)
    val maxViewBox = Rectangle.union(aligned.map(SvgLens.viewBox.get))
    val resized = aligned.map(SvgLens.viewBox.set(maxViewBox))
    interpolatePairwise(resized, options)
  }
}
