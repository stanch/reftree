package reftree.svg

import reftree.geometry._

object SvgGraphAnimation {
  private def improveRendering(svg: xml.Node): xml.Node = svg.asInstanceOf[xml.Elem] %
    new xml.UnprefixedAttribute("shape-rendering", "geometricPrecision", xml.Null)

  private def fixTextColor(svg: xml.Node) = SvgGraphLens.nodes.modify { nodes ⇒
    // Graphviz does not set the fill-opacity attribute on text
    nodes map { case (id, node) ⇒ id → SvgGraphLens.color.modify(identity)(node) }
  }(svg)

  private def align(prev: xml.Node, next: xml.Node) = {
    val prevNodes = SvgGraphLens.nodes.get(prev)
    val nextNodes = SvgGraphLens.nodes.get(next)
    val deltas = (prevNodes.keySet & nextNodes.keySet) map { id ⇒
      SvgGraphLens.nodePosition.get(prevNodes(id)) - SvgGraphLens.nodePosition.get(nextNodes(id))
    }
    // TODO: add more weight to label nodes
    val translation = Point.sum(deltas.toSeq) * (1.0 / deltas.size)
    val withBox = SvgLens.viewBox.modify(_ + translation)(next)
    (SvgGraphLens.graph composeLens SvgLens.translation).modify(_ + translation)(withBox)
  }

  private def alignPairwise(svgs: Seq[xml.Node]) =
    svgs.foldLeft(Vector(svgs.head)) {
      case ((acc :+ prev), next) ⇒ acc :+ prev :+ align(prev, next)
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

  private def interpolatePairwise(svgs: Seq[xml.Node], interpolationFrames: Int) =
    svgs.head +: svgs.sliding(2).toSeq.flatMap {
      case Seq(prev, next) ⇒
        interpolation.sample(prev, next, interpolationFrames, inclusive = false) :+ next
    }

  def animate(interpolationFrames: Int)(svgs: Seq[xml.Node]) = {
    val preprocessed = svgs.map(improveRendering).map(fixTextColor)
    val aligned = alignPairwise(preprocessed)
    val maxViewBox = Rectangle.union(aligned.map(SvgLens.viewBox.get))
    val resized = aligned.map(SvgLens.viewBox.set(maxViewBox))
    // TODO: resurrect accentDiff functionality
    interpolatePairwise(resized, interpolationFrames)
  }
}
