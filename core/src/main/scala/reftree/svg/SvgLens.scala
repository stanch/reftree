package reftree.svg

import monocle.{Iso, Lens, Optional}
import reftree.geometry.{Color, Point, Rectangle}

import scala.collection.immutable.ListMap
import scala.xml.UnprefixedAttribute
import scala.xml.transform.{RewriteRule, RuleTransformer}

object SvgLens {
  val viewBox = Lens[xml.Node, Rectangle] { svg ⇒
    Rectangle.fromString((svg \ "@viewBox").text)
  } { viewBox ⇒ svg ⇒
    svg.asInstanceOf[xml.Elem] %
      new UnprefixedAttribute("viewBox", s"$viewBox", xml.Null) %
      new UnprefixedAttribute("width", s"${viewBox.width}pt", xml.Null) %
      new UnprefixedAttribute("height", s"${viewBox.height}pt", xml.Null)
  }

  def attr(attr: String) = Lens[xml.Node, Option[String]] { svg ⇒
    svg.attribute(attr).map(_.text)
  } { value ⇒ svg ⇒
    svg.asInstanceOf[xml.Elem].copy(
      attributes = value.fold(svg.attributes.remove(attr)) { v ⇒
        svg.attributes append new UnprefixedAttribute(attr, v, xml.Null)
      }
    )
  }

  val translation = attr("transform") composeIso
    Iso[Option[String], Point] {
      case None ⇒ Point.zero
      case Some(transform) ⇒
        Point.fromString("translate\\((.+)\\)".r.findFirstMatchIn(transform).get.group(1))
    } { translation ⇒
      Some(s"translate($translation)")
    }

  val opacity = attr("opacity") composeIso
    Iso[Option[String], Double](_.fold(1.0)(_.toDouble))(o ⇒ Some(o.toString))

  def thickness(elems: Set[String]) = Optional[xml.Node, Double](Function.const(None)) { thickness ⇒ svg ⇒
    new RuleTransformer(new RewriteRule {
      override def transform(n: xml.Node): Seq[xml.Node] = n match {
        case e @ xml.Elem(_, elem, attrs, _, _*) if elems(elem) ⇒
          val elem = e.asInstanceOf[xml.Elem]
          if (!e.attribute("stroke").exists(_.text != "none")) elem else {
            elem % new UnprefixedAttribute("stroke-width", thickness.toString, xml.Null)
          }
        case other ⇒ other
      }
    }).apply(svg)
  }

  def color(elems: Set[String], fill: Boolean, stroke: Boolean) = Optional[xml.Node, Color](Function.const(None)) { color ⇒ svg ⇒
    new RuleTransformer(new RewriteRule {
      val fillAttr = xml.MetaData.concatenate(
        new UnprefixedAttribute("fill", color.toRgbString, xml.Null),
        new UnprefixedAttribute("fill-opacity", color.a.toString, xml.Null)
      )
      val strokeAttr = xml.MetaData.concatenate(
        new UnprefixedAttribute("stroke", color.toRgbString, xml.Null),
        new UnprefixedAttribute("stroke-opacity", color.a.toString, xml.Null)
      )
      override def transform(n: xml.Node): Seq[xml.Node] = n match {
        case e @ xml.Elem(_, elem, attrs, _, _*) if elems(elem) ⇒
          val elem = e.asInstanceOf[xml.Elem]
          val filled = if (!fill || !e.attribute("fill").exists(_.text != "none")) elem else {
            elem % fillAttr
          }
          if (!stroke || !e.attribute("stroke").exists(_.text != "none")) filled else {
            filled % strokeAttr
          }
        case other ⇒ other
      }
    }).apply(svg)
  }

  def singleChild(
    elem: String,
    cls: Option[String] = None,
    id: Option[String] = None
  ): Lens[xml.Node, xml.Node] = Lens[xml.Node, xml.Node] { svg ⇒
    val translation = this.translation.get(svg)
    val child = (svg \\ elem)
      .filter(e ⇒ cls.forall(_ == (e \ "@class").text))
      .filter(e ⇒ id.forall(_ == (e \ "@id").text))
      .head
    this.translation.modify(_ + translation)(child)
  } { child ⇒ svg ⇒
    val translation = this.translation.get(svg)
    val translatedChild = this.translation.modify(_ - translation)(child)
    new RuleTransformer(new RewriteRule {
      override def transform(n: xml.Node): Seq[xml.Node] = n match {
        case e @ xml.Elem(_, `elem`, attrs, _, _*)
          if (cls.isEmpty || cls == attrs.get("class").map(_.text)) &&
            (id.isEmpty || id == attrs.get("id").map(_.text)) ⇒ translatedChild
        case other ⇒ other
      }
    }).apply(svg)
  }

  def childrenById(
    elem: String,
    cls: Option[String] = None
  ): Lens[xml.Node, ListMap[String, xml.Node]] = Lens[xml.Node, ListMap[String, xml.Node]] { svg ⇒
    val translation = this.translation.get(svg)
    ListMap(
      (svg \\ elem).filter(e ⇒ cls.forall(_ == (e \ "@class").text))
        .map(this.translation.modify(_ + translation))
        .map(e ⇒ (e \ "@id").text → e): _*
    )
  } { children ⇒ svg ⇒
    val translation = this.translation.get(svg)
    val translatedChildren = children.mapValues(this.translation.modify(_ - translation))
    val ids = (svg \\ elem).filter(e ⇒ cls.forall(_ == (e \ "@class").text)).map(e ⇒ (e \ "@id").text).toSet
    val toRemove = ids diff translatedChildren.keySet
    val toAdd = translatedChildren.filterKeys(id ⇒ !ids(id)).values
    val updatedSvg = new RuleTransformer(new RewriteRule {
      override def transform(n: xml.Node): Seq[xml.Node] = n match {
        case e @ xml.Elem(_, `elem`, attrs, _, _*) if cls.isEmpty || cls == attrs.get("class").map(_.text) ⇒
          val id = attrs("id").text
          if (toRemove(id)) Seq.empty
          else translatedChildren(id)
        case other ⇒ other
      }
    }).apply(svg)
    updatedSvg.asInstanceOf[xml.Elem].copy(child = updatedSvg.child ++ toAdd)
  }
}
