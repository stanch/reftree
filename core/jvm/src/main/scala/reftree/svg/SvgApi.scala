package reftree.svg

import monocle.{Getter, Lens, Optional}

/**
 * An implementation of [[BaseSvgApi]] for scala-xml [[xml.Node]]
 */
object SvgApi extends SimpleSvgApi[xml.Node] {
  def elementName = Getter(_.label)

  def optAttr(attr: String): Lens[xml.Node, Option[String]] =
    Lens[xml.Node, Option[String]] { node ⇒
      node.attribute(attr).map(_.text)
    } { value ⇒ node ⇒
      node.asInstanceOf[xml.Elem].copy(
        attributes = value.fold(node.attributes.remove(attr)) { v ⇒
          node.attributes append new xml.UnprefixedAttribute(attr, v, xml.Null)
        }
      )
    }

  def attr(attr: String): Lens[xml.Node, String] =
    Lens[xml.Node, String] { node ⇒
      node.attribute(attr).map(_.text).get
    } { value ⇒ node ⇒
      node.asInstanceOf[xml.Elem].copy(
        attributes = node.attributes append new xml.UnprefixedAttribute(attr, value, xml.Null)
      )
    }

  def prefixedAttr(uri: String, attr: String): Lens[xml.Node, Option[String]] =
    Lens[xml.Node, Option[String]] { node ⇒
      node.attribute(uri, attr).map(_.text)
    } { value ⇒ node ⇒
      node.asInstanceOf[xml.Elem].copy(
        // TODO: how to remove a prefixed attribute?
        attributes = value.fold(node.attributes.remove(attr)) { v ⇒
          node.attributes append new xml.PrefixedAttribute(uri, attr, v, xml.Null)
        }
      )
    }

  def immediateChildren: Optional[xml.Node, List[xml.Node]] =
    Optional[xml.Node, List[xml.Node]] {
      case xml.Elem(_, _, _, _, children @ _*) if children.nonEmpty ⇒ Some(children.toList)
      case _ ⇒ None
    } (children ⇒ {
      case elem: xml.Elem ⇒ elem.copy(child = children)
      case other ⇒ other
    })
}
