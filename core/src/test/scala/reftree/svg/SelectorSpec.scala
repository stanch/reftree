package reftree.svg

import org.scalatest.{Matchers, FlatSpec}

class SelectorSpec extends FlatSpec with Matchers {
  it should "parse selectors correctly" in {
    Selector.fromString("g.node, .edge.edgier, path") shouldEqual
      Selector(Set(
        Selector.Clause(Some("g"), Set("node")),
        Selector.Clause(None, Set("edge", "edgier")),
        Selector.Clause(Some("path"), Set.empty)
      ))
  }
}
