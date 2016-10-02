package reftree

import monocle.{Iso, Lens}
import reftree.Diagram.AnimationOptions
import reftree.Geometry._

import scala.collection.immutable.ListMap
import scala.util.Try
import scala.xml.{UnprefixedAttribute, Elem}
import scala.xml.transform.{RuleTransformer, RewriteRule}

object Svgs {
  private val viewBoxLens = Lens[xml.Node, Rectangle] { svg ⇒
    Rectangle.fromString((svg \ "@viewBox").text, " ", " ")
  } { viewBox ⇒ svg ⇒
    svg.asInstanceOf[xml.Elem] %
      new UnprefixedAttribute("viewBox", s"$viewBox", xml.Null) %
      new UnprefixedAttribute("width", s"${viewBox.width}pt", xml.Null) %
      new UnprefixedAttribute("height", s"${viewBox.height}pt", xml.Null)
  }

  private def attrLens(attr: String) = Lens[xml.Node, Option[String]] { svg ⇒
    svg.attribute(attr).map(_.text)
  } { value ⇒ svg ⇒
    svg.asInstanceOf[xml.Elem].copy(
      attributes = value.fold(svg.attributes.remove(attr)) { v ⇒
        svg.attributes append new UnprefixedAttribute(attr, v, xml.Null)
      }
    )
  }

  private val translationLens = attrLens("transform") composeIso
    Iso[Option[String], Point] {
      case None ⇒ Point.zero
      case Some(transform) ⇒
        Point.fromString("translate\\((.+)\\)".r.findFirstMatchIn(transform).get.group(1), " ")
    } { translation ⇒
      Some(s"translate($translation)")
    }

  private val opacityLens = attrLens("opacity") composeIso
    Iso[Option[String], Double](_.fold(1.0)(_.toDouble))(o ⇒ Some(o.toString))

  private def childLens(elem: String, cls: String): Lens[xml.Node, ListMap[String, xml.Node]] =
    Lens[xml.Node, ListMap[String, xml.Node]] { svg ⇒
      val translation = translationLens.get(svg)
      ListMap(
        (svg \\ elem).filter(e ⇒ (e \ "@class").text == cls)
          .map(translationLens.modify(_ + translation))
          .map(e ⇒ (e \ "@id").text → e): _*
      )
    } { children ⇒ svg ⇒
      val translation = translationLens.get(svg)
      val translatedChildren = children.mapValues(translationLens.modify(_ - translation))
      val ids = (svg \\ elem).filter(e ⇒ (e \ "@class").text == cls).map(e ⇒ (e \ "@id").text).toSet
      val toRemove = ids diff translatedChildren.keySet
      val toAdd = translatedChildren.filterKeys(id ⇒ !ids(id)).values
      val updatedSvg = new RuleTransformer(new RewriteRule {
        override def transform(n: xml.Node): Seq[xml.Node] = n match {
          case e @ Elem(_, `elem`, attrs, _, _*) if attrs.get("class").map(_.text).contains(cls) ⇒
            val id = attrs("id").text
            if (toRemove(id)) Seq.empty
            else translatedChildren(id)
          case other ⇒ other
        }
      }).apply(svg)
      updatedSvg.asInstanceOf[xml.Elem].copy(child = updatedSvg.child ++ toAdd)
    }

  private def childLens(elem: String, cls: String, id: String): Lens[xml.Node, xml.Node] =
    childLens(elem, cls) composeLens Lens[ListMap[String, xml.Node], xml.Node] { map ⇒
      map(id)
    } { child ⇒ map ⇒
      map.updated(id, child)
    }

  private val graphLens = childLens("g", "graph", "graph0")
  private def nodeLens(id: String) = graphLens composeLens childLens("g", "node", id)
  private val nodesLens = graphLens composeLens childLens("g", "node")
  private val edgesLens = graphLens composeLens childLens("g", "edge")

  private val nodePositionLens = Lens[xml.Node, Point] { node ⇒
    val translation = translationLens.get(node)
    val text = (node \\ "text").head
    Point((text \ "@x").text.toDouble, (text \ "@y").text.toDouble) + translation
  } { position ⇒ node ⇒
    val text = (node \\ "text").head
    val pos = Point((text \ "@x").text.toDouble, (text \ "@y").text.toDouble)
    translationLens.set(position - pos)(node)
  }

  private def align(prev: xml.Node, next: xml.Node, prevAnchorId: String, nextAnchorId: String) = Try {
    val prevPosition = (nodeLens(prevAnchorId) composeLens nodePositionLens).get(prev)
    val nextPosition = (nodeLens(nextAnchorId) composeLens nodePositionLens).get(next)
    val translation = prevPosition - nextPosition
    val withBox = viewBoxLens.modify(_ + translation)(next)
    (graphLens composeLens translationLens).modify(_ + translation)(withBox)
  }.toOption

  private val interpolation: Interpolation[xml.Node] = {
    val opacity = Interpolation.double.lensBefore(opacityLens)
    val fadeIn = opacity.mapTime(_ * 2 - 1).withBefore(opacityLens.set(0.0))
    val fadeOut = opacity.mapTime(_ * 2).withAfter(opacityLens.set(0.0))

    val nodeOption = Interpolation.option(
      fadeOut, fadeIn, Interpolation.foldLeftBefore(
        opacity,
        Interpolation.point.lensBefore(nodePositionLens)
      )
    )

    val edgeOption = Interpolation.option(fadeOut, fadeIn, opacity)

    Interpolation.foldLeftBefore(
      Interpolation.map(nodeOption).lensBefore(nodesLens),
      Interpolation.map(edgeOption).lensBefore(edgesLens)
    )
  }

  def animate(svgs: Seq[xml.Node], anchorIds: Seq[String], options: AnimationOptions) = {
    val aligned = (svgs.tail zip anchorIds.sliding(2).toSeq).foldLeft(Vector(svgs.head)) {
      case (acc :+ prev, (next, Seq(prevAnchorId, nextAnchorId))) ⇒
        val anchoringAttempt = if (!options.anchoring) None else {
          align(prev, next, prevAnchorId, prevAnchorId)
        }
        lazy val default = align(prev, next, prevAnchorId, nextAnchorId).get
        acc :+ prev :+ (anchoringAttempt getOrElse default)
    }
    val maxViewBox = Rectangle.union(aligned.map(viewBoxLens.get))
    val resized = aligned.map(viewBoxLens.set(maxViewBox))
    val interpolated = Seq.fill(options.interpolationFrames + 1)(resized.head) ++
      resized.sliding(2).toSeq.flatMap {
        case Seq(prev, next) ⇒
          Seq.tabulate(options.interpolationFrames) { i ⇒
            interpolation(prev, next, (i + 1).toDouble / (options.interpolationFrames + 1))
          } ++ Seq.fill(options.interpolationFrames + 1)(next)
      }
    interpolated
  }
}
