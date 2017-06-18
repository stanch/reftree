package reftree.svg.api

import monocle._
import reftree.geometry._
import reftree.util.Optics
import zipper.Unzip

/**
 * SVG API sufficient to implement animations
 */
abstract class BaseSvgApi[Svg] {
  /* Basic element properties and selection */

  def elementName: Getter[Svg, String]
  def elementId: Getter[Svg, Option[String]]
  def elementClasses: Getter[Svg, Set[String]]

  final def select(selector: String): Prism[Svg, Svg] = Optics.only { svg ⇒
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
  private def translated[A <: Svg, B](optional: Optional[A, B])(implicit translatable: Translatable[B]) =
    Optional[A, B] { svg ⇒
      val t = translation.getOption(svg).getOrElse(Point.zero)
      optional.getOption(svg).map(translatable.translate(_, t))
    } { value ⇒ svg ⇒
      val t = translation.getOption(svg).getOrElse(Point.zero)
      optional.set(translatable.translate(value, -t))(svg)
    }

  private implicit lazy val svgTranslatable: Translatable[Svg] =
    Translatable((svg, delta) ⇒ translation.modify(_ + delta)(svg))

  final def groupPosition(anchor: Optional[Svg, Point]): Optional[Svg, Point] =
    Optional[Svg, Point](anchor.getOption) { position ⇒ svg ⇒
      anchor.getOption(svg).fold(svg) { anchorPosition ⇒
        translation.modify(_ + position - anchorPosition)(svg)
      }
    }

  /* Navigation */

  def immediateChildren: Optional[Svg, List[Svg]]

  final lazy val realImmediateChildren = translated(immediateChildren)

  final implicit lazy val svgUnzip: Unzip[Svg] =
    Optics.unzip(realImmediateChildren)

  /* Misc attributes */

  def shapeRendering: Optional[Svg, Option[String]]
  def textRendering: Optional[Svg, Option[String]]
  def viewBox: Optional[Svg, Rectangle]
  def opacity: Optional[Svg, Double]
  def fillColor: Optional[Svg, Option[Color]]
  def strokeColor: Optional[Svg, Option[Color]]
  def strokeWidth: Optional[Svg, Double]

  /* Prisms for particular SVG elements */

  final val polygons = select("polygon")
  final val paths = select("path")
  final val texts = select("text")
  final val anchors = select("a")

  /* Element-specific attributes */

  def polygonPoints: Optional[Svg, Polyline]
  def pathPath: Optional[Svg, Path]
  def textPosition: Optional[Svg, Point]

  final lazy val realPolygonPoints = translated(polygonPoints)
  final lazy val realPathPath = translated(pathPath)
  final lazy val realTextPosition = translated(textPosition)

  def anchorTitle: Optional[Svg, Option[String]]
}
