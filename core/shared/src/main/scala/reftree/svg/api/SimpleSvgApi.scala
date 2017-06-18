package reftree.svg.api

import monocle.{Getter, Iso, Optional}
import reftree.geometry._
import reftree.util.Optics
import reftree.util.Optics.RichOptional

/**
 * An implementation of [[BaseSvgApi]] that only requires
 * defining the optics for attributes and navigation
 */
trait SimpleSvgApi[Node] extends BaseSvgApi[Node] {
  def optAttr(attr: String): Optional[Node, Option[String]]
  def attr(attr: String): Optional[Node, String]
  def prefixedAttr(uri: String, attr: String): Optional[Node, Option[String]]

  val elementId = optAttr("id").asOptionGetter composeGetter
    Getter(_.flatten)

  val elementClasses = optAttr("class").asOptionGetter composeGetter
    Getter(_.flatten.fold(Set.empty[String])(_.split(' ').toSet))

  lazy val translation = select("g, path, polygon, text, a") composeOptional
    optAttr("transform") composeIso
    Iso[Option[String], Point] {
      case None ⇒ Point.zero
      case Some(transform) ⇒
        Point.fromString("translate\\((.+)\\)".r.findFirstMatchIn(transform).get.group(1))
    } {
      case Point.`zero` ⇒ None
      case t ⇒ Some(s"translate($t)")
    }

  val shapeRendering = optAttr("shape-rendering")
  val textRendering = optAttr("text-rendering")

  val viewBox = Optics.tupleOptionalLeft(
    attr("viewBox"),
    optAttr("width"),
    optAttr("height")
  ) composeIso
    Iso[(String, Option[String], Option[String]), Rectangle] {
      case (box, _, _) ⇒ Rectangle.fromString(box)
    } { viewBox ⇒
      (s"$viewBox", Some(s"${viewBox.width}pt"), Some(s"${viewBox.height}pt"))
    }

  val opacity = optAttr("opacity") composeIso
    Iso[Option[String], Double](_.fold(1.0)(_.toDouble))(o ⇒ Some(o.toString))

  private def color(fillOrStroke: String) =
    Optics.tupleOptionalLeft(
      optAttr(fillOrStroke),
      optAttr(s"$fillOrStroke-opacity")
    ) composeIso
    Iso[(Option[String], Option[String]), Option[Color]] {
      case (Some("none"), _) | (None, _) ⇒ None
      case (Some(color), alpha) ⇒ Some(Color.fromRgbaString(color, alpha.fold(1.0)(_.toDouble)))
    } {
      case None ⇒ (Some("none"), None)
      case Some(color) ⇒ (Some(color.toRgbString), Some(color.a.toString))
    }

  val fillColor = color("fill")
  val strokeColor = color("stroke")

  val strokeWidth = optAttr("stroke-width") composeIso
    Iso[Option[String], Double](_.fold(1.0)(_.toDouble))(t ⇒ Some(t.toString))

  val polygonPoints = polygons composeOptional
    attr("points") composeIso Polyline.stringIso

  val pathPath = paths composeOptional
    attr("d") composeIso Path.stringIso

  val textPosition = texts composeOptional
    Optics.tupleOptionalLeft(attr("x"), attr("y")) composeIso Point.stringPairIso

  val anchorTitle = anchors composeOptional
    prefixedAttr("http://www.w3.org/1999/xlink", "title")
}
