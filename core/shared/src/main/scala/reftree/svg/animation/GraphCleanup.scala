package reftree.svg.animation

import reftree.svg.api.BaseSvgApi
import reftree.util.Optics

case class GraphCleanup[Svg](api: BaseSvgApi[Svg]) {
  import api.svgUnzip

  /** Prevent ugly rendering artifacts */
  def improveRendering(svg: Svg): Svg =
    api.shapeRendering.set(Some("geometricPrecision")) andThen
    api.textRendering.set(Some("geometricPrecision")) apply svg

  private val nodes = Optics.collectAllLeft(api.select("g.node"))
  private val texts = Optics.collectAllLeft(api.select("text"))
  private val nodeColor = Optics.collectLast(api.select("path")) composeOptional api.strokeColor

  /**
   * Graphviz does not set the fill-opacity attribute on text,
   * so we copy it from the sibling path node.
   */
  def fixTextColor(svg: Svg): Svg =
    nodes.modify { node ⇒
      val color = nodeColor.getOption(node)
      color.fold(node)(c ⇒ texts.modify(api.fillColor.set(c))(node))
    }(svg)

  def cleanup(svgs: Stream[Svg]): Stream[Svg] = svgs
    .map(improveRendering).map(fixTextColor)
}
