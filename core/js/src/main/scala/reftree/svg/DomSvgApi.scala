package reftree.svg

import monocle.macros.GenPrism
import monocle.{Getter, Lens, Optional}
import org.scalajs.dom
import reftree.svg.api.SimpleSvgApi

/**
 * An implementation of [[api.BaseSvgApi]] for scala-js-dom [[dom.Node]]
 */
object DomSvgApi extends SimpleSvgApi[dom.Node] {
  val elementName = Getter[dom.Node, String](_.nodeName)

  lazy val element = GenPrism[dom.Node, dom.Element]

  def optAttr(attr: String): Optional[dom.Node, Option[String]] =
    element composeLens
    Lens[dom.Element, Option[String]] { elem ⇒
      Option(elem.getAttribute(attr))
    } { value ⇒ elem ⇒
      val clone = elem.cloneNode(deep = true).asInstanceOf[dom.Element]
      value.fold(clone.removeAttribute(attr))(clone.setAttribute(attr, _))
      clone
    }

  def attr(attr: String): Optional[dom.Node, String] =
    element composeLens
    Lens[dom.Element, String] { elem ⇒
      elem.getAttribute(attr)
    } { value ⇒ elem ⇒
      val clone = elem.cloneNode(deep = true).asInstanceOf[dom.Element]
      clone.setAttribute(attr, value)
      clone
    }

  def prefixedAttr(uri: String, attr: String): Optional[dom.Node, Option[String]] =
    element composeLens
    Lens[dom.Element, Option[String]] { elem ⇒
      Option(elem.getAttributeNS(uri, attr))
    } { value ⇒ elem ⇒
      val clone = elem.cloneNode(deep = true).asInstanceOf[dom.Element]
      value.fold(clone.removeAttributeNS(uri, attr))(clone.setAttributeNS(uri, attr, _))
      clone
    }

  def immediateChildren: Optional[dom.Node, List[dom.Node]] =
    element composeLens
    Lens[dom.Element, List[dom.Node]](_.childNodes.toList) { children ⇒ elem ⇒
      val clone = elem.cloneNode(deep = false).asInstanceOf[dom.Element]
      children.foreach(clone.appendChild)
      clone
    }
}
