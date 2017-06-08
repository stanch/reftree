package reftree.svg

import monocle.{Getter, Lens, Optional}
import org.scalajs.dom
import org.scalajs.dom.ext.PimpedNodeList

/**
 * An implementation of [[BaseSvgApi]] for scala-js-dom [[dom.Node]]
 */
object SvgApi extends SimpleSvgApi[dom.Node] {
  def elementName = Getter(_.nodeName)

  def optAttr(attr: String): Lens[dom.Node, Option[String]] =
    Lens[dom.Node, Option[String]] { node ⇒
      Option(node.asInstanceOf[dom.Element].getAttribute(attr))
    } { value ⇒ node ⇒
      val clone = node.cloneNode(deep = true).asInstanceOf[dom.Element]
      value.fold(clone.removeAttribute(attr))(clone.setAttribute(attr, _))
      clone
    }

  def attr(attr: String): Lens[dom.Node, String] =
    Lens[dom.Node, String] { node ⇒
      node.asInstanceOf[dom.Element].getAttribute(attr)
    } { value ⇒ node ⇒
      val clone = node.cloneNode(deep = true).asInstanceOf[dom.Element]
      clone.setAttribute(attr, value)
      clone
    }

  def prefixedAttr(uri: String, attr: String): Lens[dom.Node, Option[String]] =
    Lens[dom.Node, Option[String]] { node ⇒
      Option(node.asInstanceOf[dom.Element].getAttributeNS(uri, attr))
    } { value ⇒ node ⇒
      val clone = node.cloneNode(deep = true).asInstanceOf[dom.Element]
      value.fold(clone.removeAttributeNS(uri, attr))(clone.setAttributeNS(uri, attr, _))
      clone
    }

  def immediateChildren: Optional[dom.Node, List[dom.Node]] =
    Optional[dom.Node, List[dom.Node]] {
      case elem: dom.Element if elem.childElementCount > 0 ⇒ Some(elem.childNodes.toList)
      case _ ⇒ None
    } (children ⇒ {
      case elem: dom.Element ⇒
        val clone = elem.cloneNode(deep = false).asInstanceOf[dom.Element]
        children.foreach(clone.appendChild)
        clone
      case other ⇒ other
    })
}
