package reftree.diagram

import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import reftree.diagram.Animation.Builder

class AnimationSpec extends FlatSpec with PropertyChecks with Matchers {

  "iterateUntilFixPoint" should "expand until fixpoint" in {

    forAll { seed: Int =>
      whenever(seed > 0) {
        val fixPointBuilder =
          Builder(seed).iterateUntilFixPoint(_ / 2)
        val expectedSize = (math.log(seed) / math.log(2)) + 2 //first + last frame
        fixPointBuilder.frames should have size expectedSize.toInt
      }
    }

  }

  "iterateUntilFixPointAtMost" should "expand until fixpoint or max" in {

    forAll { seed: Int =>
      whenever(seed > 0) {
        val max = 5
        val fixPointBuilder =
          Builder(seed).iterateUntilFixPointAtMost(max)(_ / 2)
        val expectedSize = math.min(max, (math.log(seed) / math.log(2)) + 2) //first + last frame
        fixPointBuilder.frames should have size expectedSize.toInt
      }
    }

  }

  "iterateUntil" should "expand until some predicate is true" in {

    val fixPointBuilder = Builder(0).iterateUntil(_ + 13)(_ > 100)
    fixPointBuilder.frames shouldEqual Vector(0, 13, 26, 39, 52, 65, 78, 91,
      104)

  }

  "iterateUntilAtMost" should "expand until some predicate is true or max" in {

    val max = 5
    val fixPointBuilder = Builder(0).iterateUntilAtMost(max)(_ + 13)(_ > 100)
    fixPointBuilder.frames shouldEqual Vector(0, 13, 26, 39, 52)

    val fixPointSmall = Builder(0).iterateUntilAtMost(max)(_ + 13)(_ > 30)
    fixPointSmall.frames shouldEqual Vector(0, 13, 26, 39)

  }
}
