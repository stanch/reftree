package reftree.contrib

import reftree.core._

/**
 * [[ToRefTree]] instances for [[xml.Node]]
 */
object XmlInstances {
  private def xmlRefTree(e: xml.Node): Option[RefTree] = e match {
    case xml.Elem(_, _, attributes, _, children @ _*) ⇒
      val attrFields = attributes.iterator.toSeq map { attr ⇒
        attr.value.head.refTree.toField.withName(attr.prefixedKey)
      }
      val childFields = children.flatMap(xmlRefTree).map(_.toField)
      Some(RefTree.Ref(e, attrFields ++ childFields).rename(s"<${e.label}>"))
    case xml.Text(text) ⇒
      Some(RefTree.Ref(e, Seq.empty).rename(s""""$text""""))
    case _ ⇒
      None
  }

  implicit val `XML Node RefTree`: ToRefTree[xml.Node] =
    ToRefTree[xml.Node] { node ⇒
      xmlRefTree(node) getOrElse RefTree.Null()
    }

  implicit val `XML Elem RefTree`: ToRefTree[xml.Elem] =
    ToRefTree[xml.Elem](`XML Node RefTree`.refTree)

  implicit val `Xml Marker` = new OpticInstances.Marker[xml.Node]({
    case e: xml.Elem ⇒ e % new xml.UnprefixedAttribute("marked---", "true", xml.Null)
    case x ⇒ x
  })
}
