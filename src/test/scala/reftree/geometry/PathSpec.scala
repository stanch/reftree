package reftree.geometry

import org.scalatest.{Matchers, FlatSpec}

class PathSpec extends FlatSpec with Matchers {
  it should "parse SVG paths" in {
    val pathString = "M89,-288.5C89,-202.361 57.5417,-169.948 100,-95 123.033,-54.3423 184.732,-78.8751 191.411,-39.9227"

    val path = Path(Seq(
      PathSegment.Move(Point(89, -288.5)),
      PathSegment.Bezier(
        Point(89, -288.5), Point(89, -202.361),
        Point(57.5417, -169.948), Point(100, -95)
      ),
      PathSegment.Bezier(
        Point(100, -95), Point(123.033, -54.3423),
        Point(184.732, -78.8751), Point(191.411, -39.9227)
      )
    ))

    Path.fromString(pathString) shouldEqual path
  }
}
