package reftree.contrib

import reftree.core._

/**
 * [[ToRefTree]] instances for [[xml.Node]]
 */
object XmlInstances {
  private def xmlRefTree(e: xml.Node)(implicit stringAsTree: ToRefTree[String]): Option[RefTree] = e match {
    case xml.Elem(_, _, attributes, _, children @ _*) =>
      val attrFields = attributes.iterator.toSeq map { attr =>
        attr.value.head.refTree.toField.withName(attr.prefixedKey)
      }
      val childFields = children.flatMap(xmlRefTree(_)).map(_.toField)
      Some(RefTree.Ref(e, attrFields ++ childFields).rename(s"<${e.label}>"))
    case xml.Text(text) =>
      val nodeTree = RefTree.Ref(e, Seq.empty)
      val textTree = stringAsTree.refTree(text)
      Some(textTree.asInstanceOf[RefTree.Ref].copy(id = nodeTree.id))
    case _ =>
      None
  }

  implicit def `XML Node RefTree`(implicit stringAsTree: ToRefTree[String]): ToRefTree[xml.Node] =
    ToRefTree[xml.Node] { node =>
      xmlRefTree(node) getOrElse RefTree.Null()
    }

  implicit def `XML Elem RefTree`(implicit stringAsTree: ToRefTree[String]): ToRefTree[xml.Elem] =
    ToRefTree[xml.Elem](`XML Node RefTree`.refTree)

  implicit val `Xml Marker`: OpticInstances.Marker[xml.Node] = new OpticInstances.Marker[xml.Node]({
    case e: xml.Elem => e % new xml.UnprefixedAttribute("marked---", "true", xml.Null)
    case x => x
  })
}
