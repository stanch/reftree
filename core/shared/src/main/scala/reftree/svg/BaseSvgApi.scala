package reftree.svg

import monocle._
import reftree.geometry._
import reftree.util.Optics
import zipper.Unzip

/**
 * SVG API sufficient to implement animations, i.e. [[SvgGraphAnimation]]
 */
abstract class BaseSvgApi[Svg] {
  /* SVG types */

  type SvgPolygon <: Svg
  type SvgPath <: Svg
  type SvgText <: Svg
  type SvgAnchor <: Svg

  /* Basic element properties and selection */

  def elementName: Getter[Svg, String]
  def elementId: Getter[Svg, Option[String]]
  def elementClasses: Getter[Svg, Set[String]]

  def select(selector: String): Prism[Svg, Svg] = Optics.only { svg ⇒
    Selector.fromString(selector).clauses.exists { clause ⇒
      clause.element.forall(_ == elementName.get(svg)) &&
      clause.classes.subsetOf(elementClasses.get(svg))
    }
  }

  /* Translation */

  def translation: Optional[Svg, Point]

  /**
   * Makes sure that translation is propagated when moving up and down the SVG nodes,
   * as well as when obtaining attributes that have coordinates inside.
   */
  protected def translated[A <: Svg, B](lens: Lens[A, B])(implicit translatable: Translatable[B]) =
    Lens[A, B] { svg ⇒
      val t = translation.getOption(svg).getOrElse(Point.zero)
      translatable.translate(lens.get(svg), t)
    } { value ⇒ svg ⇒
      val t = translation.getOption(svg).getOrElse(Point.zero)
      lens.set(translatable.translate(value, -t))(svg)
    }

  /**
   * Makes sure that translation is propagated when moving up and down the SVG nodes,
   * as well as when obtaining attributes that have coordinates inside.
   */
  protected def translated[A <: Svg, B](optional: Optional[A, B])(implicit translatable: Translatable[B]) =
    Optional[A, B] { svg ⇒
      val t = translation.getOption(svg).getOrElse(Point.zero)
      optional.getOption(svg).map(translatable.translate(_, t))
    } { value ⇒ svg ⇒
      val t = translation.getOption(svg).getOrElse(Point.zero)
      optional.set(translatable.translate(value, -t))(svg)
    }

  implicit lazy val svgTranslatable: Translatable[Svg] =
    Translatable((svg, delta) ⇒ translation.modify(_ + delta)(svg))

  def groupPosition(anchor: Optional[Svg, Point]): Optional[Svg, Point] =
    Optional[Svg, Point](anchor.getOption) { position ⇒ svg ⇒
      anchor.getOption(svg).fold(svg) { anchorPosition ⇒
        translation.modify(_ + position - anchorPosition)(svg)
      }
    }

  /* Navigation */

  def immediateChildren: Optional[Svg, List[Svg]]

  implicit lazy val svgUnzip: Unzip[Svg] =
    Optics.unzip(translated(immediateChildren))

  /* Misc attributes */

  def shapeRendering: Lens[Svg, Option[String]]
  def textRendering: Lens[Svg, Option[String]]
  def viewBox: Lens[Svg, Rectangle]
  def opacity: Lens[Svg, Double]
  def fillColor: Lens[Svg, Option[Color]]
  def strokeColor: Lens[Svg, Option[Color]]
  def strokeWidth: Lens[Svg, Double]

  /* Prisms for particular SVG elements */

  def polygons: Prism[Svg, SvgPolygon]
  def paths: Prism[Svg, SvgPath]
  def texts: Prism[Svg, SvgText]
  def anchors: Prism[Svg, SvgAnchor]

  /* Element-specific attributes */

  def polygonPoints: Lens[SvgPolygon, Polyline]
  def pathPath: Lens[SvgPath, Path]
  def textPosition: Lens[SvgText, Point]
  def anchorTitle: Lens[SvgAnchor, Option[String]]
}
