package reftree

package object svg {
  /** Syntax sugar for [[Selector]] */
  implicit class SelectorSyntax(ctx: StringContext) {
    def sel(args: Any*) = Selector.fromString(ctx.s(args: _*))
  }
}
