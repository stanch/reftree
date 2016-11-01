package reftree.svg

import monocle.Lens
import reftree.geometry._

import scala.collection.immutable.ListMap

object SvgGraphAnimation {
  private def improveRendering(svg: xml.Node): xml.Node = svg.asInstanceOf[xml.Elem] %
    new xml.UnprefixedAttribute("shape-rendering", "geometricPrecision", xml.Null)

  private def fixTextColor(svg: xml.Node) = SvgGraphLens.nodes.modify { nodes ⇒
    // Graphviz does not set the fill-opacity attribute on text
    nodes map { case (id, node) ⇒ id → SvgGraphLens.color.modify(identity)(node) }
  }(svg)

  private def accentuatePairwise(svgs: Seq[xml.Node]) = {
    def accentuate(
      prev: xml.Node, next: xml.Node,
      lens: Lens[xml.Node, ListMap[String, xml.Node]]
    ) = {
      val prevNodes = lens.get(prev)
      lens.modify(_ map {
        case (id, node) if !prevNodes.contains(id) && !id.contains("-caption-") ⇒
          (id, SvgGraphLens.thickness.modify(_ * 1.6)(node))
        case other ⇒ other
      })(next)
    }
    svgs.head +: svgs.sliding(2).toSeq.map {
      case Seq(prev, next) ⇒
        accentuate(prev, accentuate(prev, next, SvgGraphLens.nodes), SvgGraphLens.edges)
    } :+ svgs.last
  }

  private def align(prev: xml.Node, next: xml.Node) = {
    def groupByAnchor(nodes: Map[String, xml.Node]) =
      nodes.values.groupBy(SvgGraphLens.nodeAnchor.get) flatMap {
        case (Some(anchor), group) ⇒ Some((anchor, group.head))
        case _ ⇒ None
      }

    def mapDelta(prev: Map[String, xml.Node], next: Map[String, xml.Node]) = {
      val common = prev.keySet & next.keySet
      if (common.isEmpty) None else {
        val deltas = common map { id ⇒
          SvgGraphLens.nodePosition.get(prev(id)) -
            SvgGraphLens.nodePosition.get(next(id))
        }
        Some(Point.mean(deltas.toSeq))
      }
    }

    def seqDelta(prev: Seq[xml.Node], next: Seq[xml.Node]) = {
      Point.mean(prev.map(SvgGraphLens.nodePosition.get)) -
        Point.mean(next.map(SvgGraphLens.nodePosition.get))
    }

    val prevNodes = SvgGraphLens.nodes.get(prev)
    val nextNodes = SvgGraphLens.nodes.get(next)
    val prevAnchors = groupByAnchor(prevNodes)
    val nextAnchors = groupByAnchor(nextNodes)
    val translation =
      mapDelta(prevAnchors, nextAnchors) orElse
      mapDelta(prevNodes, nextNodes) getOrElse
      seqDelta(prevNodes.values.toSeq, nextNodes.values.toSeq)
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

    val opacityColorThickness = Interpolation.combineLeft(
      Interpolation.double.lensLeft(SvgLens.opacity).timespan(1/3.0, 2/3.0),
      Color.interpolation.lensLeft(SvgGraphLens.color).timespan(1/3.0, 1),
      Interpolation.double.lensLeft(SvgGraphLens.thickness).timespan(0, 1)
    )

    val nodePosition = Point.interpolation.lensLeft(SvgGraphLens.nodePosition)
      .timespan(1/3.0, 2/3.0)

    val nodeHighlight = Interpolation.option(
      Color.interpolation.withRight(_.opacify(0)).timespan(0, 1/2.0),
      Color.interpolation.withLeft(_.opacify(0)).timespan(1/2.0, 1),
      Color.interpolation
    ).lensLeft(SvgGraphLens.nodeHighlight)

    val edgePosition = Interpolation.combineLeft(
      Polyline.interpolation.lensLeft(SvgGraphLens.edgeArrow),
      Path.interpolation.lensLeft(SvgGraphLens.edgePath)
    ).timespan(1/3.0, 2/3.0)

    val node = Interpolation.option(
      fadeOut, fadeIn, Interpolation.combineLeft(opacityColorThickness, nodeHighlight, nodePosition)
    )

    val edge = Interpolation.option(
      fadeOut, fadeIn, Interpolation.combineLeft(opacityColorThickness, edgePosition)
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
    val preprocessed = accentuatePairwise(svgs.map(improveRendering).map(fixTextColor))
    val aligned = alignPairwise(preprocessed)
    val maxViewBox = Rectangle.union(aligned.map(SvgLens.viewBox.get))
    val resized = aligned.map(SvgLens.viewBox.set(maxViewBox))
    interpolatePairwise(resized, interpolationFrames)
  }
}
