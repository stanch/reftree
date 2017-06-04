package reftree.svg

import monocle.{Getter, Iso}
import reftree.geometry._
import reftree.util.Optics

/**
 * An implementation of [[SvgApi]] for scala-xml [[xml.Node]]
 */
object ScalaXmlSvgApi extends SvgApi[xml.Node] {
  import Optics.{xmlOptAttr, xmlAttr}

  type SvgPolygon = xml.Node
  type SvgPath = xml.Node
  type SvgText = xml.Node
  type SvgAnchor = xml.Node

  def elementName = Getter(_.label)
  def elementId = xmlOptAttr("id").asGetter
  def elementClasses: Getter[xml.Node, Set[String]] =
    xmlOptAttr("class") composeGetter Getter(_.fold(Set.empty[String])(_.split(' ').toSet))

  val translation = Optics.only(select("g, path, polygon, text, a")) composeLens
    xmlOptAttr("transform") composeIso
    Iso[Option[String], Point] {
      case None ⇒ Point.zero
      case Some(transform) ⇒
        Point.fromString("translate\\((.+)\\)".r.findFirstMatchIn(transform).get.group(1))
    } {
      case Point.`zero` ⇒ None
      case t ⇒ Some(s"translate($t)")
    }

  def immediateChildren = Optics.xmlImmediateChildren

  def shapeRendering = xmlOptAttr("shape-rendering")
  def textRendering = xmlOptAttr("text-rendering")

  val viewBox = Optics.tupleLensLeft(
    xmlAttr("viewBox"),
    xmlOptAttr("width"),
    xmlOptAttr("height")
  ) composeIso
    Iso[(String, Option[String], Option[String]), Rectangle] {
      case (box, _, _) ⇒ Rectangle.fromString(box)
    } { viewBox ⇒
      (s"$viewBox", Some(s"${viewBox.width}pt"), Some(s"${viewBox.height}pt"))
    }

  val opacity = xmlOptAttr("opacity") composeIso
    Iso[Option[String], Double](_.fold(1.0)(_.toDouble))(o ⇒ Some(o.toString))

  private def color(fillOrStroke: String) =
    Optics.tupleLensLeft(
      xmlOptAttr(fillOrStroke),
      xmlOptAttr(s"$fillOrStroke-opacity")
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

  val strokeWidth = xmlOptAttr("stroke-width") composeIso
    Iso[Option[String], Double](_.map(_.toDouble).getOrElse(1.0))(t ⇒ Some(t.toString))

  def polygons = Optics.only(select("polygon"))
  def paths = Optics.only(select("path"))
  def texts = Optics.only(select("text"))
  def anchors = Optics.only(select("a"))

  val polygonPoints = translated {
    xmlAttr("points") composeIso Polyline.stringIso
  }

  val pathPath = translated {
    xmlAttr("d") composeIso Path.stringIso
  }

  val textPosition = translated {
    Optics.tupleLensLeft(xmlAttr("x"), xmlAttr("y")) composeIso Point.stringPairIso
  }

  val anchorTitle = Optics.xmlPrefixedAttribute("http://www.w3.org/1999/xlink", "title")
}
