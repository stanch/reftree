package reftree.dot

import reftree.geometry.Color
import reftree.dot.html.Html

/**
 * A simple representation of a graph in the dot language
 * @see http://www.graphviz.org/doc/info/lang.html
 */
case class Graph(
  strict: Boolean,
  directed: Boolean,
  statements: Seq[GraphStatement]
) {
  def encode = DotEncoding.encode(this)

  def nodes = statements collect { case n: Node ⇒ n }
  def edges = statements collect { case e: Edge ⇒ e }
}

object Graph {
  case class Attrs(
    rankSep: Option[Double] = None
  ) extends GraphStatement
}

sealed trait GraphStatement

case class Node(
  id: String,
  label: Html,
  attrs: Node.Attrs = Node.Attrs()
) extends GraphStatement

object Node {
  case class Attrs(
    shape: Option[String] = None,
    tooltip: Option[String] = None,
    color: Option[Color] = None,
    fontName: Option[String] = None,
    fontColor: Option[Color] = None
  ) extends GraphStatement
}

case class NodeId(
  id: String,
  portId: Option[String] = None,
  compassPoint: Option[String] = None
) {
  def withPort(id: String) = copy(portId = Some(id))
  def north = copy(compassPoint = Some("n"))
  def south = copy(compassPoint = Some("s"))
}

case class Edge(
  from: NodeId,
  to: NodeId,
  id: String,
  attrs: Edge.Attrs = Edge.Attrs()
) extends GraphStatement

object Edge {
  case class Attrs(
    arrowSize: Option[Double] = None,
    color: Option[Color] = None
  ) extends GraphStatement
}
