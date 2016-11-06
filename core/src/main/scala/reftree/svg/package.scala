package reftree

package object svg {
  implicit class SelectorSyntax(ctx: StringContext) {
    def sel(args: Any*) = Selector.fromString(ctx.s(args: _*))
  }
}
