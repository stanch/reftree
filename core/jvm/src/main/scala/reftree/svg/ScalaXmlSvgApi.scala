package reftree.svg

import monocle.{Getter, Iso}
import reftree.geometry._
import reftree.util.Optics

/**
 * An implementation of [[SvgApi]] for scala-xml [[xml.Node]]
 */
object ScalaXmlSvgApi extends SvgApi[xml.Node] {
  import XmlOptics.{attr, optAttr, prefixedAttr}

  type SvgPolygon = xml.Node
  type SvgPath = xml.Node
  type SvgText = xml.Node
  type SvgAnchor = xml.Node

  def elementName = Getter(_.label)
  def elementId = optAttr("id").asGetter
  def elementClasses: Getter[xml.Node, Set[String]] =
    optAttr("class") composeGetter Getter(_.fold(Set.empty[String])(_.split(' ').toSet))

  val translation = select("g, path, polygon, text, a") composeLens
    optAttr("transform") composeIso
    Iso[Option[String], Point] {
      case None ⇒ Point.zero
      case Some(transform) ⇒
        Point.fromString("translate\\((.+)\\)".r.findFirstMatchIn(transform).get.group(1))
    } {
      case Point.`zero` ⇒ None
      case t ⇒ Some(s"translate($t)")
    }

  def immediateChildren = XmlOptics.immediateChildren

  def shapeRendering = optAttr("shape-rendering")
  def textRendering = optAttr("text-rendering")

  val viewBox = Optics.tupleLensLeft(
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
    Optics.tupleLensLeft(
      optAttr(fillOrStroke),
      optAttr(s"$fillOrStroke-opacity")
    ) composeIso
    Iso[(Option[String], Option[String]), Option[Color]] {
      case (Some("none"), _) | (None, _) ⇒ None
      case (Some(color), alpha) ⇒ Some(Color.fromRgbaString(color, alpha.map(_.toDouble).getOrElse(1.0)))
    } {
      case None ⇒ (Some("none"), None)
      case Some(color) ⇒ (Some(color.toRgbString), Some(color.a.toString))
    }

  val fillColor = color("fill")

  val strokeColor = color("stroke")

  val strokeWidth = optAttr("stroke-width") composeIso
    Iso[Option[String], Double](_.map(_.toDouble).getOrElse(1.0))(t ⇒ Some(t.toString))

  def polygons = select("polygon")
  def paths = select("path")
  def texts = select("text")
  def anchors = select("a")

  val polygonPoints = translated {
    attr("points") composeIso Polyline.stringIso
  }

  val pathPath = translated {
    attr("d") composeIso Path.stringIso
  }

  val textPosition = translated {
    Optics.tupleLensLeft(attr("x"), attr("y")) composeIso Point.stringPairIso
  }

  val anchorTitle = prefixedAttr("http://www.w3.org/1999/xlink", "title")
}
