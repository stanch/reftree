package reftree.svg

import monocle.{Getter, Lens}
import com.softwaremill.quicklens._
import reftree.geometry.{Color, Point, Polyline, Path}

import scala.xml.UnprefixedAttribute

object SvgGraphLens {
  val graph = SvgLens.singleChild("g", Some("graph"), Some("graph0"))
  def node(id: String) = graph composeLens SvgLens.singleChild("g", Some("node"), Some(id))
  val nodes = graph composeLens SvgLens.childrenById("g", Some("node"))
  val edges = graph composeLens SvgLens.childrenById("g", Some("edge"))

  private def highlightPath(svg: xml.Node) =
    svg.label == "path" && (svg \ "@stroke").text == "none"

  val color = Lens[xml.Node, Color.RGBA] { nodeOrEdge ⇒
    (nodeOrEdge \\ "path").filterNot(highlightPath).headOption map { path ⇒
      val opacity = path.attribute("stroke-opacity").map(_.text.toDouble).getOrElse(1.0)
      Color.RGBA.fromString((path \ "@stroke").text, opacity)
    } getOrElse {
      val text = (nodeOrEdge \\ "text").head
      val color = text.attribute("fill").map(_.text).getOrElse("#000000")
      Color.RGBA.fromString(color)
    }
  } { color ⇒
    SvgLens.color(Set("text", "polygon"), fill = true, stroke = true).set(color) andThen
    SvgLens.color(Set("path"), fill = false, stroke = true).set(color)
  }

  val nodeAnchor = Getter[xml.Node, Option[String]] { node ⇒
    (node \\ "a" \ "@{http://www.w3.org/1999/xlink}title").headOption.map(_.text)
  }

  val nodePosition = Lens[xml.Node, Point] { node ⇒
    val translation = SvgLens.translation.get(node)
    val text = (node \\ "text").head
    Point((text \ "@x").text.toDouble, (text \ "@y").text.toDouble) + translation
  } { position ⇒ node ⇒
    val text = (node \\ "text").head
    val pos = Point((text \ "@x").text.toDouble, (text \ "@y").text.toDouble)
    SvgLens.translation.set(position - pos)(node)
  }

  val nodeHighlight = Lens[xml.Node, Option[Color.RGBA]] { node ⇒
    (node \\ "path").find(highlightPath) map { path ⇒
      val opacity = path.attribute("fill-opacity").map(_.text.toDouble).getOrElse(1.0)
      Color.RGBA.fromString((path \ "@fill").text, opacity)
    }
  } {
    case Some(color) ⇒ node ⇒
      val highlightedNode = if ((node \\ "path").exists(highlightPath)) node else {
        // create a new path for highlighting by copying the existing outline
        // the fill color will be properly set below
        val strokeAttr = new UnprefixedAttribute("stroke", "none", xml.Null)
        val fillAttr = new UnprefixedAttribute("fill", "#000000", xml.Null)
        val path = (node \\ "path").head.asInstanceOf[xml.Elem] % strokeAttr % fillAttr
        node.asInstanceOf[xml.Elem].modify(_.child).using(path +: _)
      }
      SvgLens.color(Set("path"), fill = true, stroke = false).set(color)(highlightedNode)
    case None ⇒ node ⇒
      // remove the highlight path from the node
      node.asInstanceOf[xml.Elem].modify(_.child).using(_.filterNot(highlightPath))
  }

  val edgePath = SvgLens.singleChild("path") composeLens Lens[xml.Node, Path] { path ⇒
    val translation = SvgLens.translation.get(path)
    Path.fromString((path \ "@d").text) + translation
  } { d ⇒ path ⇒
    val translation = SvgLens.translation.get(path)
    val translatedD = d - translation
    path.asInstanceOf[xml.Elem] % new UnprefixedAttribute("d", translatedD.toString, xml.Null)
  }

  val edgeArrow = SvgLens.singleChild("polygon") composeLens Lens[xml.Node, Polyline] { poly ⇒
    val translation = SvgLens.translation.get(poly)
    Polyline.fromString((poly \ "@points").text) + translation
  } { arrow ⇒ poly ⇒
    val translation = SvgLens.translation.get(poly)
    val translatedArrow = arrow - translation
    poly.asInstanceOf[xml.Elem] % new UnprefixedAttribute("points", translatedArrow.toString, xml.Null)
  }
}
