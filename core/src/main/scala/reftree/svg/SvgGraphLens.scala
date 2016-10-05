package reftree.svg

import monocle.Lens
import reftree.geometry.{Point, Polyline, Path}

import scala.xml.UnprefixedAttribute

object SvgGraphLens {
  val graph = SvgLens.singleChild("g", Some("graph"), Some("graph0"))
  def node(id: String) = graph composeLens SvgLens.singleChild("g", Some("node"), Some(id))
  val nodes = graph composeLens SvgLens.childrenById("g", Some("node"))
  val edges = graph composeLens SvgLens.childrenById("g", Some("edge"))

  val nodePosition = Lens[xml.Node, Point] { node ⇒
    val translation = SvgLens.translation.get(node)
    val text = (node \\ "text").head
    Point((text \ "@x").text.toDouble, (text \ "@y").text.toDouble) + translation
  } { position ⇒ node ⇒
    val text = (node \\ "text").head
    val pos = Point((text \ "@x").text.toDouble, (text \ "@y").text.toDouble)
    SvgLens.translation.set(position - pos)(node)
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
