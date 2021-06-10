package reftree.svg.api

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SelectorSpec extends AnyFlatSpec with Matchers {
  it should "parse selectors correctly" in {
    Selector.fromString("g.node, .edge.edgier, path") shouldEqual
      Selector(Set(
        Selector.Clause(Some("g"), Set("node")),
        Selector.Clause(None, Set("edge", "edgier")),
        Selector.Clause(Some("path"), Set.empty)
      ))
  }
}
