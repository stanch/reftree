package reftree.geometry

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class ColorSpec extends FlatSpec with Matchers with PropertyChecks {
  val e = 0.005

  def sameRgba(left: Color.RGBA, right: Color.RGBA) =
    (Color.rgbaComponents.get(left) zip Color.rgbaComponents.get(right)) foreach {
      case (x, y) ⇒ x shouldEqual y +- e
    }

  def sameHsla(left: Color.HSLA, right: Color.HSLA) =
    (Color.hslaComponents.get(left) zip Color.hslaComponents.get(right)) foreach {
      case (x, y) ⇒ x shouldEqual y +- e
    }

  val genColor = for {
    a ← Gen.choose(0.0, 1.0)
    b ← Gen.choose(0.0, 1.0)
    c ← Gen.choose(0.0, 1.0)
    d ← Gen.choose(0.0, 1.0)
  } yield Seq(a, b, c, d)

  val genRgba = genColor.map(Color.rgbaComponents.apply)
  val genHsla = genColor.map(Color.hslaComponents.apply)

  it should "parse RGBA colors" in {
    val colors = Table(
      "string" → "color",
      "#104e8b" → Color.RGBA(0x10 / 255.0, 0x4e / 255.0, 0x8b / 255.0, 1.0),
      "#228b22" → Color.RGBA(0x22 / 255.0, 0x8b / 255.0, 0x22 / 255.0, 1.0)
    )
    forAll(colors)((string, color) ⇒ sameRgba(color, Color.fromRgbaString(string)))
  }

  it should "convert RGBA to HSLA" in {
    val colors = Table(
      "rgba" → "hsla",
      "#104e8b" → Color.HSLA(210 / 360.0, 0.79, 0.3, 1.0),
      "#228b22" → Color.HSLA(120 / 360.0, 0.61, 0.34, 1.0)
    )
    forAll(colors)((string, color) ⇒ sameHsla(color, Color.fromRgbaString(string).toHsla))
  }

  it should "convert from RGBA to string and back" in {
    forAll(genRgba)(color ⇒ sameRgba(color, Color.fromRgbaString(color.toRgbString, color.a)))
  }

  it should "convert from RGBA to HSLA and back" in {
    forAll(genRgba)(color ⇒ sameRgba(color, color.toHsla.toRgba))
  }

  it should "convert from HSLA to RGBA and back" in {
    forAll(genHsla)(color ⇒ sameHsla(color, color.toRgba.toHsla))
  }
}
