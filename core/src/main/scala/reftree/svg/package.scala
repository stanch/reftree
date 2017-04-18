package reftree

import reftree.geometry.{Point, Translatable}
import reftree.util.Optics
import zipper.Unzip

package object svg {
  /** Syntax sugar for [[Selector]] */
  implicit class SelectorSyntax(ctx: StringContext) {
    def sel(args: Any*) = Selector.fromString(ctx.s(args: _*))
  }

  implicit object `SVG Translatable` extends Translatable[xml.Node] {
    def translate(value: xml.Node, delta: Point) =
      SvgOptics.translation.modify(_ + delta)(value)
  }

  implicit val `SVG Unzip`: Unzip[xml.Node] =
    Optics.unzip(SvgOptics.translated(Optics.xmlImmediateChildren))
}
