package reftree.graph

/**
 * A simple representation of a graph in the dot language
 * @see http://www.graphviz.org/doc/info/lang.html
 */
case class Graph(
  strict: Boolean,
  directed: Boolean,
  statements: GraphStatement*
) {
  override def toString = {
    def a(attrs: Seq[Attr]) = attrs
      .map(a ⇒ s"${a.name} = ${a.value}")
      .mkString(" [", "; ", "]")

    def nodeId(nodeId: NodeId) =
      (Seq(nodeId.id) ++ nodeId.portId ++ nodeId.compassPoint).mkString(":")

    val s = if (strict) "strict " else ""
    val d = if (directed) "digraph" else "graph"

    val t = statements.map {
      case node: Node ⇒
        node.id + a(node.attrs)
      case edge: Edge ⇒
        nodeId(edge.from) + (if (directed) " -> " else " -- ") + nodeId(edge.to) + a(edge.attrs)
      case attrs: Attrs ⇒
        attrs.getClass.getSimpleName.toLowerCase + a(attrs.attrs)
    }.mkString(" {\n  ", ";\n  ", "\n}")

    s + d + t
  }
}

/** A dot identifier which can be encoded as a string */
sealed trait Identifier {
  def encoded: String
  override def toString = encoded
}

object Identifier {
  case class Str(value: String) extends Identifier {
    def encoded = s""""${value.replace("\"", "\\\"")}""""
  }

  case class Num(value: BigDecimal) extends Identifier {
    def encoded = s"$value"
  }

  case class Html(value: xml.Node) extends Identifier {
    def encoded = s"<$value>"
  }
}

/** A typeclass for types that can be used as dot identifiers */
trait ToIdentifier[A] {
  def id(value: A): Identifier
}

object ToIdentifier {
  def apply[A](f: A ⇒ Identifier) = new ToIdentifier[A] {
    def id(value: A) = f(value)
  }

  implicit val `String Id` = ToIdentifier[String](Identifier.Str)
  implicit val `Int Id` = ToIdentifier[Int](n ⇒ Identifier.Num(BigDecimal(n)))
  implicit val `Double Id` = ToIdentifier[Double](n ⇒ Identifier.Num(BigDecimal(n)))
  implicit val `Html Id` = ToIdentifier[xml.Elem](Identifier.Html)
}

case class Attr(name: String, value: Identifier)

object Attr {
  /** Convenient := syntax for specifying attributes */
  implicit class AttrSyntax(val name: String) extends AnyVal {
    def :=[A: ToIdentifier](value: A): Attr =
      Attr(name, implicitly[ToIdentifier[A]].id(value))
  }
}

sealed trait GraphStatement

case class Node(id: Identifier, attrs: Seq[Attr]) extends GraphStatement {
  def addAttrs(attrs: Attr*) = copy(attrs = this.attrs ++ attrs)
}

object Node {
  def apply[A: ToIdentifier](id: A, attrs: Attr*): Node =
    Node(implicitly[ToIdentifier[A]].id(id), attrs)
}

case class NodeId(id: Identifier, portId: Option[Identifier], compassPoint: Option[String]) {
  def withPort[A: ToIdentifier](id: A) =
    copy(portId = Some(implicitly[ToIdentifier[A]].id(id)))

  def north = copy(compassPoint = Some("n"))
  def south = copy(compassPoint = Some("s"))
}

object NodeId {
  def apply[A: ToIdentifier](id: A): NodeId =
    NodeId(implicitly[ToIdentifier[A]].id(id), None, None)
}

case class Edge(from: NodeId, to: NodeId, attrs: Attr*) extends GraphStatement

sealed trait Attrs extends GraphStatement {
  def attrs: Seq[Attr]
}

object Attrs {
  case class Node(attrs: Attr*) extends Attrs
  case class Edge(attrs: Attr*) extends Attrs
  case class Graph(attrs: Attr*) extends Attrs
}
