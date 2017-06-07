package reftree.dot

import reftree.dot.html.{HtmlEncoding, Html}
import reftree.geometry.Color
import shapeless.LabelledProductTypeClassCompanion

/**
 * Encoding the DOT AST into its stringified representation
 */
case class DotEncoding[A](encoding: A ⇒ Chunk[Graph]) extends Encoding[Graph, A]

/**
 * Automatically derives the encoding for case classes with graph/node/edge attributes
 */
private[dot] sealed trait DotAttrEncoding extends LabelledProductTypeClassCompanion[DotEncoding] {
  import shapeless._

  object typeClass extends LabelledProductTypeClass[DotEncoding] {
    def emptyProduct: DotEncoding[HNil] =
      new DotEncoding(_ ⇒ Chunk.empty[Graph])

    def product[H, T <: HList](
      name: String,
      ch: DotEncoding[H],
      ct: DotEncoding[T]
    ): DotEncoding[H :: T] = new DotEncoding({
      case head :: tail ⇒
        val headEnc = ch.encoding(head)
          // TODO: we are assuming that an empty attribute should be always omitted
          if (headEnc.encoded.isEmpty) {
            ct.encoding(tail)
          } else {
            Chunk.join(" ")(
              Chunk.join("=")(Chunk(name.toLowerCase), headEnc),
              ct.encoding(tail)
            )
          }
    })

    def project[F, G](instance: ⇒ DotEncoding[G], to: F ⇒ G, from: G ⇒ F): DotEncoding[F] =
      new DotEncoding(value ⇒ instance.encoding(to(value)).wrap("[ ", " ]"))
  }
}

object DotEncoding extends EncodingCompanion[Graph, DotEncoding] with DotAttrEncoding {
  implicit val `String Enc` = new DotEncoding[String](x ⇒ Chunk(s""""${x.replace("\"", "\\\"")}""""))
  implicit val `Int Enc` = new DotEncoding[Int](x ⇒ Chunk(x.toString))
  implicit val `Double Enc` = new DotEncoding[Double](x ⇒ Chunk(x.toString))
  implicit val `Color Enc` = new DotEncoding[Color](x ⇒ x.toRgbaString.encoded)
  implicit val `Html Enc` = new DotEncoding[Html](x ⇒ Chunk(s"<${HtmlEncoding.encode(x)}>"))

  implicit def `Option Enc`[A: DotEncoding]: DotEncoding[Option[A]] = new DotEncoding({
    case Some(value) ⇒ value.encoded
    case None ⇒ Chunk.empty[Graph]
  })

  case class NodeExtraAttrs(id: String, label: Html)
  case class EdgeExtraAttrs(id: String)

  val root = new DotEncoding[Graph]({ graph ⇒
    implicit val nodeIdEnc = new DotEncoding[NodeId]({ nodeId ⇒
      Chunk.join(":") {
        (Seq(nodeId.id.encoded) ++
          nodeId.portId.map(_.encoded) ++
          nodeId.compassPoint.map(_.encoded)): _*
      }
    })

    val statementEnc = new DotEncoding[GraphStatement]({
      case node: Node ⇒
        val extra = NodeExtraAttrs(node.id, node.label)
        Chunk.join(" ")(node.id.encoded, extra.encoded, node.attrs.encoded)
      case edge: Edge ⇒
        val extra = EdgeExtraAttrs(edge.id)
        Chunk.join(" ")(
          Chunk.join(if (graph.directed) " -> " else " -- ")(edge.from.encoded, edge.to.encoded),
          extra.encoded,
          edge.attrs.encoded
        )
      case graphAttrs: Graph.Attrs ⇒
        Chunk.join(" ")(raw("graph"), graphAttrs.encoded)
      case nodeAttrs: Node.Attrs ⇒
        Chunk.join(" ")(raw("node"), nodeAttrs.encoded)
      case edgeAttrs: Edge.Attrs ⇒
        Chunk.join(" ")(raw("edge"), edgeAttrs.encoded)
    })

    val s = if (graph.strict) raw("strict") else raw("")
    val d = if (graph.directed) raw("digraph") else raw("graph")

    val content = Chunk.join("\n")(graph.statements.map(statementEnc.encoding).map(_.wrap("  ", "")): _*)
      .wrap("{\n", "\n}")

    Chunk.join(" ")(s, d, content)
  })
}
