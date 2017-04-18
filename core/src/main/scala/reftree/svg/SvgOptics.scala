package reftree.svg

import monocle.{Iso, Optional}
import reftree.geometry._
import reftree.util.Optics

object SvgOptics { optics ⇒
  import Optics.{only, xmlAttribute, xmlMandatoryAttribute}

  val viewBox = Optics.tupleLensLeft(
    xmlMandatoryAttribute("viewBox"),
    xmlAttribute("width"),
    xmlAttribute("height")
  ) composeIso
    Iso[(String, Option[String], Option[String]), Rectangle] {
      case (box, _, _) ⇒ Rectangle.fromString(box)
    } { viewBox ⇒
      (s"$viewBox", Some(s"${viewBox.width}pt"), Some(s"${viewBox.height}pt"))
    }

  val translation = only(sel"g, path, polygon, text, a") composeLens
    xmlAttribute("transform") composeIso
    Iso[Option[String], Point] {
      case None ⇒ Point.zero
      case Some(transform) ⇒
        Point.fromString("translate\\((.+)\\)".r.findFirstMatchIn(transform).get.group(1))
    } {
      case Point.`zero` ⇒ None
      case translation ⇒ Some(s"translate($translation)")
    }

  val opacity = xmlAttribute("opacity") composeIso
    Iso[Option[String], Double](_.fold(1.0)(_.toDouble))(o ⇒ Some(o.toString))

  private def color(fillOrStroke: String) =
    Optics.tupleLensLeft(
      xmlAttribute(fillOrStroke),
      xmlAttribute(s"$fillOrStroke-opacity")
    ) composeIso
    Iso[(Option[String], Option[String]), Option[Color]] {
      case (Some("none"), _) | (None, _) ⇒ None
      case (Some(color), alpha) ⇒ Some(Color.fromRgbaString(color, alpha.map(_.toDouble).getOrElse(1.0)))
    } {
      case None ⇒ (Some("none"), None)
      case Some(color) ⇒ (Some(color.toRgbString), Some(color.a.toString))
    }

  val fillColor = only(sel"path, polygon, text") composeLens
    color("fill")

  val strokeColor = only(sel"path, polygon, text") composeLens
    color("stroke")

  val thickness = only(sel"path, polygon") composeLens
    xmlAttribute("stroke-width") composeIso
    Iso[Option[String], Double](_.map(_.toDouble).getOrElse(1.0))(t ⇒ Some(t.toString))

  /**
   * Makes sure that translation is propagated when moving up and down the SVG nodes,
   * as well as when obtaining attributes that have coordinates inside.
   */
  def translated[A](optional: Optional[xml.Node, A])(implicit t: Translatable[A]) =
    Optional[xml.Node, A] { svg ⇒
      val translation = optics.translation.getOption(svg).getOrElse(Point.zero)
      optional.getOption(svg).map(t.translate(_, translation))
    } { value ⇒ svg ⇒
      val translation = optics.translation.getOption(svg).getOrElse(Point.zero)
      optional.set(t.translate(value, -translation))(svg)
    }

  val polygonPoints = translated {
    only(sel"polygon") composeLens
    xmlMandatoryAttribute("points") composeIso
    Polyline.stringIso
  }

  val path = translated {
    only(sel"path") composeLens
    xmlMandatoryAttribute("d") composeIso
    Path.stringIso
  }

  val textPosition = translated {
    only(sel"text") composeLens
    Optics.tupleLensLeft(xmlMandatoryAttribute("x"), xmlMandatoryAttribute("y")) composeIso
    Point.stringPairIso
  }

  def groupPosition(anchor: Optional[xml.Node, Point]) =
    only(sel"g") composeOptional
    Optional[xml.Node, Point](anchor.getOption) { position ⇒ svg ⇒
      anchor.getOption(svg).fold(svg) { anchorPosition ⇒
        optics.translation.modify(_ + position - anchorPosition)(svg)
      }
    }
}
