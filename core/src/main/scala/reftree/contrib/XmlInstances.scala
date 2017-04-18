package reftree.contrib

import reftree.core._

/**
 * [[ToRefTree]] instances for [[xml.Node]]
 */
object XmlInstances {
  private def xmlRefTree(e: xml.Node)(
    implicit stringToRefTree: ToRefTree[String]
  ): Option[RefTree] = e match {
    case xml.Elem(_, _, attributes, _, children @ _*) ⇒
      val attrFields = attributes.asAttrMap.toSeq map { case (k, v) ⇒ v.refTree.toField.withName(k) }
      val childFields = children.flatMap(xmlRefTree(_)).map(_.toField)
      Some(RefTree.Ref(e, attrFields ++ childFields).rename(s"<${e.label}>"))
    case xml.Text(text) ⇒
      Some(text.refTree)
    case _ ⇒
      None
  }

  implicit def `XML RefTree`(implicit stringToRefTree: ToRefTree[String]): ToRefTree[xml.Node] =
    ToRefTree[xml.Node] { node ⇒
      xmlRefTree(node) getOrElse RefTree.Null()
    }
}
