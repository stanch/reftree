package reftree.svg.api

import monocle.Optional
import monocle.macros.GenLens
import reftree.geometry.{Color, Path, Point, Polyline}
import reftree.util.Optics.{RichLens, RichOptional}

case class SvgWrapper[Svg](
  api: BaseSvgApi[Svg],
  svg: Svg,
  classes: Set[String],
  children: Option[List[SvgWrapper[Svg]]],
  translation: Option[Point],
  opacity: Option[Double],
  fillColor: Option[Option[Color]],
  strokeColor: Option[Option[Color]],
  strokeWidth: Option[Double],
  polygonPoints: Option[Polyline],
  pathPath: Option[Path],
  textPosition: Option[Point]
) {
  def unwrap: Svg = {
    api.immediateChildren.setOption(children.map(_.map(_.unwrap))) _ andThen
    api.translation.setOption(translation) andThen
    api.opacity.setOption(opacity) andThen
    api.fillColor.setOption(fillColor) andThen
    api.strokeColor.setOption(strokeColor) andThen
    api.strokeWidth.setOption(strokeWidth) andThen
    api.polygonPoints.setOption(polygonPoints) andThen
    api.pathPath.setOption(pathPath) andThen
    api.textPosition.setOption(textPosition) apply svg
  }
}

object SvgWrapper {
  def wrap[Svg](api: BaseSvgApi[Svg])(svg: Svg): SvgWrapper[Svg] = SvgWrapper(
    api,
    svg,
    api.elementClasses.get(svg),
    api.immediateChildren.getOption(svg).map(_.map(wrap(api))),
    api.translation.getOption(svg),
    api.opacity.getOption(svg),
    api.fillColor.getOption(svg),
    api.strokeColor.getOption(svg),
    api.strokeWidth.getOption(svg),
    api.polygonPoints.getOption(svg),
    api.pathPath.getOption(svg),
    api.textPosition.getOption(svg)
  )
}

/**
 * An implementation of [[BaseSvgApi]] that optimizes another existing implementation
 * by caching the node attributes and avoiding excessive parsing/stringifying
 */
case class OptimizedSvgApi[Svg](api: BaseSvgApi[Svg]) extends BaseSvgApi[SvgWrapper[Svg]] {
  private val underlying = GenLens[SvgWrapper[Svg]](_.svg)

  val elementName = underlying composeGetter api.elementName
  val elementId = underlying composeGetter api.elementId
  val elementClasses = GenLens[SvgWrapper[Svg]](_.classes).asGetter

  lazy val translation: Optional[SvgWrapper[Svg], Point] =
    GenLens[SvgWrapper[Svg]](_.translation).asFlatOptional

  val immediateChildren: Optional[SvgWrapper[Svg], List[SvgWrapper[Svg]]] =
    GenLens[SvgWrapper[Svg]](_.children).asFlatOptional

  val shapeRendering = underlying composeOptional api.shapeRendering
  val textRendering = underlying composeOptional api.textRendering
  val viewBox = underlying composeOptional api.viewBox

  val opacity: Optional[SvgWrapper[Svg], Double] =
    GenLens[SvgWrapper[Svg]](_.opacity).asFlatOptional

  val fillColor: Optional[SvgWrapper[Svg], Option[Color]] =
    GenLens[SvgWrapper[Svg]](_.fillColor).asFlatOptional

  val strokeColor: Optional[SvgWrapper[Svg], Option[Color]] =
    GenLens[SvgWrapper[Svg]](_.strokeColor).asFlatOptional

  val strokeWidth: Optional[SvgWrapper[Svg], Double] =
    GenLens[SvgWrapper[Svg]](_.strokeWidth).asFlatOptional

  val polygonPoints: Optional[SvgWrapper[Svg], Polyline] =
    GenLens[SvgWrapper[Svg]](_.polygonPoints).asFlatOptional

  val pathPath: Optional[SvgWrapper[Svg], Path] =
    GenLens[SvgWrapper[Svg]](_.pathPath).asFlatOptional

  val textPosition: Optional[SvgWrapper[Svg], Point] =
    GenLens[SvgWrapper[Svg]](_.textPosition).asFlatOptional

  val anchorTitle = underlying composeOptional api.anchorTitle
}
