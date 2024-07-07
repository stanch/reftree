package reftree.svg

import monocle.macros.GenPrism
import monocle.{Getter, Lens, Optional}
import reftree.svg.api.SimpleSvgApi

/**
 * An implementation of [[api.BaseSvgApi]] for scala-xml [[xml.Node]]
 */
object XmlSvgApi extends SimpleSvgApi[xml.Node] {
  val elementName = Getter[xml.Node, String](_.label)

  lazy val element = GenPrism[xml.Node, xml.Elem]

  def optAttr(attr: String): Optional[xml.Node, Option[String]] =
    element composeLens
    Lens[xml.Elem, Option[String]] { elem =>
      elem.attribute(attr).map(_.text)
    } { value => elem =>
      elem.copy(
        attributes = value.fold(elem.attributes.remove(attr)) { v =>
          elem.attributes append new xml.UnprefixedAttribute(attr, v, xml.Null)
        }
      )
    }

  def attr(attr: String): Optional[xml.Node, String] =
    element composeLens
    Lens[xml.Elem, String] { elem =>
      elem.attribute(attr).map(_.text).get
    } { value => elem =>
      elem.copy(
        attributes = elem.attributes append new xml.UnprefixedAttribute(attr, value, xml.Null)
      )
    }

  def prefixedAttr(uri: String, attr: String): Optional[xml.Node, Option[String]] =
    element composeLens
    Lens[xml.Elem, Option[String]] { elem =>
      elem.attribute(uri, attr).map(_.text)
    } { value => elem =>
      elem.copy(
        // TODO: how to remove a prefixed attribute?
        attributes = value.fold(elem.attributes.remove(attr)) { v =>
          elem.attributes append new xml.PrefixedAttribute(uri, attr, v, xml.Null)
        }
      )
    }

  val immediateChildren: Optional[xml.Node, List[xml.Node]] =
    element composeLens
    Lens[xml.Elem, List[xml.Node]](_.child.toList)(children => _.copy(child = children))
}
