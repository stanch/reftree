package reftree.dot.html

import reftree.dot.{EncodingCompanion, Encoding, Chunk}
import reftree.geometry.Color
import shapeless.LabelledTypeClassCompanion

/**
 * Encoding the DOT HTML AST into its stringified representation
 */
case class HtmlEncoding[A](encoding: A ⇒ Chunk) extends Encoding[A]

/**
 * Automatically derives the encoding for case classes with tag attributes (table, cell, ...)
 */
private[html] sealed trait HtmlAttrEncoding extends LabelledTypeClassCompanion[HtmlEncoding] {
  import shapeless._

  object typeClass extends LabelledTypeClass[HtmlEncoding] {
    def emptyCoproduct: HtmlEncoding[CNil] =
      new HtmlEncoding(_ ⇒ Chunk.empty)

    def coproduct[L, R <: Coproduct](
      name: String,
      cl: ⇒ HtmlEncoding[L],
      cr: ⇒ HtmlEncoding[R]
    ): HtmlEncoding[L :+: R] = new HtmlEncoding({
      case Inl(head) ⇒ cl.encoding(head)
      case Inr(tail) ⇒ cr.encoding(tail)
    })

    def emptyProduct: HtmlEncoding[HNil] =
      new HtmlEncoding(_ ⇒ Chunk.empty)

    def product[H, T <: HList](
      name: String,
      ch: HtmlEncoding[H],
      ct: HtmlEncoding[T]
    ): HtmlEncoding[H :: T] = new HtmlEncoding({
      case head :: tail ⇒
        val headEnc = ch.encoding(head)
        // TODO: we are assuming that an empty attribute should be always omitted
        if (headEnc.encoded.isEmpty) {
          ct.encoding(tail)
        } else {
          Chunk.join(" ")(
            Chunk.join("=")(Chunk(name.toLowerCase), headEnc.wrap("\"", "\"")),
            ct.encoding(tail)
          )
        }
    })

    def project[F, G](instance: ⇒ HtmlEncoding[G], to: F ⇒ G, from: G ⇒ F): HtmlEncoding[F] =
      new HtmlEncoding(value ⇒ instance.encoding(to(value)))
  }
}

object HtmlEncoding extends EncodingCompanion[Html, HtmlEncoding] with HtmlAttrEncoding {
  implicit val `String Enc` = new HtmlEncoding[String](value ⇒
    Chunk(value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("\'", "&apos;")
    )
  )

  implicit val `Int Enc` = new HtmlEncoding[Int](x ⇒ Chunk(x.toString))
  implicit val `Color Enc` = new HtmlEncoding[Color](x ⇒ Chunk(x.toRgbaString))

  implicit def `Option Enc`[A: HtmlEncoding]: HtmlEncoding[Option[A]] = new HtmlEncoding({
    case Some(value) ⇒ value.encoded
    case None ⇒ Chunk.empty
  })

  lazy val root: HtmlEncoding[Html] = new HtmlEncoding({
    case Raw(text) ⇒ Chunk(text)
    case Plain(text) ⇒ text.encoded
    case RowDivider ⇒ Chunk(s"<hr/>")
    case tag: Tag ⇒
      val name = tag.tagName
      val header = Chunk.join(Chunk(s"<$name"), tag.attrs.encoded.wrap(" ", ""), Chunk(">"))
      val footer = Chunk(s"</$name>")
      val content = Chunk.join(tag.children.map(root.encoding): _*)
      Chunk.join(header, content, footer)
  })
}
