package reftree

package object core {
  /** Syntax sugar for converting data to [[RefTree]] */
  implicit class RefTreeSyntax[A: core.ToRefTree](value: A) {
    def refTree = implicitly[core.ToRefTree[A]].refTree(value)
  }
}
