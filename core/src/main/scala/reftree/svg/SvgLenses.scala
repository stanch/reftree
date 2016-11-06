package reftree.svg

import monocle.{Prism, Iso, Lens, Optional}
import monocle.function.all.each
import monocle.std.list.listEach
import reftree.geometry._
import reftree.util.Lenses
import zipper._

import scala.collection.immutable.ListMap
import scala.xml.{PrefixedAttribute, UnprefixedAttribute}

object SvgLenses { lenses ⇒
  def onlyFor(selector: Selector) = Prism[xml.Node, xml.Node] { svg ⇒
    if (selector.matches(svg)) Some(svg) else None
  }(identity)

  def attr(attr: String) = Lens[xml.Node, Option[String]] { svg ⇒
    svg.attribute(attr).map(_.text)
  } { value ⇒ svg ⇒
    svg.asInstanceOf[xml.Elem].copy(
      attributes = value.fold(svg.attributes.remove(attr)) { v ⇒
        svg.attributes append new UnprefixedAttribute(attr, v, xml.Null)
      }
    )
  }

  def prefixedAttr(uri: String, attr: String) = Lens[xml.Node, Option[String]] { svg ⇒
    svg.attribute(uri, attr).map(_.text)
  } { value ⇒ svg ⇒
    svg.asInstanceOf[xml.Elem].copy(
      // TODO: how to remove a prefixed attribute?
      attributes = value.fold(svg.attributes.remove(attr)) { v ⇒
        svg.attributes append new PrefixedAttribute(uri, attr, v, xml.Null)
      }
    )
  }

  val viewBox = Lenses.tupleLensLeft(attr("viewBox"), attr("width"), attr("height")) composeIso
    Iso[(Option[String], Option[String], Option[String]), Rectangle] {
      case (box, _, _) ⇒ Rectangle.fromString(box.get)
    } { viewBox ⇒
      (Some(s"$viewBox"), Some(s"${viewBox.width}pt"), Some(s"${viewBox.height}pt"))
    }

  val translation = onlyFor(sel"g, path, polygon, text, a") composeLens
    attr("transform") composeIso
    Iso[Option[String], Point] {
      case None ⇒ Point.zero
      case Some(transform) ⇒
        Point.fromString("translate\\((.+)\\)".r.findFirstMatchIn(transform).get.group(1))
    } {
      case Point.`zero` ⇒ None
      case translation ⇒ Some(s"translate($translation)")
    }

  val opacity = attr("opacity") composeIso
    Iso[Option[String], Double](_.fold(1.0)(_.toDouble))(o ⇒ Some(o.toString))

  private def color(fillOrStroke: String) =
    Lenses.tupleLensLeft(attr(fillOrStroke), attr(s"$fillOrStroke-opacity")) composeIso
    Iso[(Option[String], Option[String]), Option[Color]] {
      case (Some("none"), _) | (None, _) ⇒ None
      case (Some(color), alpha) ⇒ Some(Color.fromRgbaString(color, alpha.map(_.toDouble).getOrElse(1.0)))
    } {
      case None ⇒ (Some("none"), None)
      case Some(color) ⇒ (Some(color.toRgbString), Some(color.a.toString))
    }

  val fillColor = onlyFor(sel"path, polygon, text") composeLens
    color("fill")

  val strokeColor = onlyFor(sel"path, polygon, text") composeLens
    color("stroke")

  val thickness = onlyFor(sel"path, polygon") composeLens
    attr("stroke-width") composeIso
    Iso[Option[String], Double](_.map(_.toDouble).getOrElse(1.0))(t ⇒ Some(t.toString))

  /**
   * Makes sure that translation is propagated when moving up and down the SVG nodes,
   * as well as when obtaining attributes that have coordinates inside.
   */
  private def translated[A](optional: Optional[xml.Node, A])(implicit t: Translatable[A]) =
    Optional[xml.Node, A] { svg ⇒
      val translation = lenses.translation.getOption(svg).getOrElse(Point.zero)
      optional.getOption(svg).map(t.translate(_, translation))
    } { value ⇒ svg ⇒
      val translation = lenses.translation.getOption(svg).getOrElse(Point.zero)
      optional.set(t.translate(value, -translation))(svg)
    }

  val polygonPoints = translated {
    onlyFor(sel"polygon") composeLens
    attr("points") composeIso
    Iso[Option[String], Polyline] { points ⇒
      Polyline.fromString(points.get)
    } { polyline ⇒
      Some(polyline.toString)
    }
  }

  val path = translated {
    onlyFor(sel"path") composeLens
    attr("d") composeIso
    Iso[Option[String], Path] { d ⇒
      Path.fromString(d.get)
    } { path ⇒
      Some(path.toString)
    }
  }

  val textPosition = translated {
    onlyFor(sel"text") composeLens
    Lenses.tupleLensLeft(attr("x"), attr("y")) composeIso
    Iso[(Option[String], Option[String]), Point] {
      case (x, y) ⇒ Point(x.get.toDouble, y.get.toDouble)
    } { point ⇒
      (Some(point.x.toString), Some(point.y.toString))
    }
  }

  def groupPosition(anchor: Optional[xml.Node, Point]) =
    onlyFor(sel"g") composeOptional
    Optional[xml.Node, Point](anchor.getOption) { position ⇒ svg ⇒
      anchor.getOption(svg).fold(svg) { anchorPosition ⇒
        lenses.translation.modify(_ + position - anchorPosition)(svg)
      }
    }

  private implicit object `SVG Translatable` extends Translatable[xml.Node] {
    def translate(value: xml.Node, delta: Point) =
      lenses.translation.modify(_ + delta)(value)
  }

  val immediateChildren = translated {
    Optional[xml.Node, List[xml.Node]] {
      case xml.Elem(_, _, _, _, children @ _*) if children.nonEmpty ⇒ Some(children.toList)
      case _ ⇒ None
    } (children ⇒ {
      case elem: xml.Elem ⇒ elem.copy(child = children)
      case other ⇒ other
    })
  }

  private implicit object `SVG Unzip` extends Unzip[xml.Node] {
    def unzip(node: xml.Node): List[xml.Node] =
      immediateChildren.getOption(node).getOrElse(List.empty)
    def zip(node: xml.Node, children: List[xml.Node]) =
      immediateChildren.set(children)(node)
  }

  // depth-first movement

  private def next[A]: Zipper.Move[A] =
    _.tryMoveDownLeft
      .orElse(_.tryMoveRight)
      .orElse(_.tryMoveUp.flatMap(_.tryMoveRight))

  private def nextRight[A]: Zipper.Move[A] =
    _.tryMoveDownRight
      .orElse(_.tryMoveLeft)
      .orElse(_.tryMoveUp.flatMap(_.tryMoveLeft))

  private def deleteAndNext[A]: Zipper.Move[A] =
    _.tryDeleteAndMoveRight
      .orElse(_.tryDeleteAndMoveUp.flatMap(_.tryMoveRight))

  private def collectOne(selector: Selector, move: Zipper.Move[xml.Node]): Optional[xml.Node, xml.Node] = {
    Optional[xml.Node, xml.Node] { svg ⇒
      Zipper(svg).tryRepeatWhileNot(selector.matches, move).toOption.map(_.focus)
    } { child ⇒ svg ⇒
      Zipper(svg).tryRepeatWhileNot(selector.matches, move).toOption.fold(svg)(_.set(child).commit)
    }
  }

  def collectFirst(selector: Selector): Optional[xml.Node, xml.Node] =
    collectOne(selector, next)

  def collectLast(selector: Selector): Optional[xml.Node, xml.Node] =
    collectOne(selector, nextRight)

  def collectById(selector: Selector): Lens[xml.Node, ListMap[String, xml.Node]] = {
    def id(e: xml.Node) = (e \ "@id").text
    Lens[xml.Node, ListMap[String, xml.Node]] {
      Zipper(_).loopAccum(ListMap.empty[String, xml.Node]) { (z, m) ⇒
        if (selector.matches(z.focus)) (next(z), m.updated(id(z.focus), z.focus))
        else (next(z), m)
      }._2
    } { children ⇒ svg ⇒
      val (zipper, remaining) = Zipper(svg).loopAccum(children) { (z, m) ⇒
        if (!selector.matches(z.focus)) (next(z), m) else {
          val i = id(z.focus)
          if (m contains i) (next(z.set(m(i))), m - i)
          else (deleteAndNext(z), m)
        }
      }
      zipper.cycle(_.tryMoveUp).insertDownRight(remaining.values.toList).commit
    }
  }

  def collectByIndex(selector: Selector): Lens[xml.Node, List[xml.Node]] = {
    Lens[xml.Node, List[xml.Node]] {
      Zipper(_).loopAccum(List.empty[xml.Node]) { (z, s) ⇒
        if (selector.matches(z.focus)) (next(z), z.focus :: s)
        else (next(z), s)
      }._2.reverse
    } { children ⇒
      Zipper(_).loopAccum(children) { (z, s) ⇒
        if (selector.matches(z.focus)) (next(z.set(s.head)), s.tail)
        else (next(z), s)
      }._1.commit
    }
  }

  def collectAll(selector: Selector) = collectByIndex(selector) composeTraversal each
}
