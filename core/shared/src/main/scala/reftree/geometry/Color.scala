package reftree.geometry

import monocle.Iso
import com.softwaremill.quicklens._

/**
 * A simple, portable color implementation that supports RGBA and HSLA
 *
 * Use `Color.fromRgbaString`, `Color.RGBA` or `Color.HSLA` to construct colors.
 */
sealed trait Color {
  /** The alpha value */
  def a: Double

  def toRgba: Color.RGBA
  def toHsla: Color.HSLA

  def lighten(factor: Double) = toHsla.modify(_.l).using(_ * factor min 1.0)

  def saturate(factor: Double) = toHsla.modify(_.s).using(_ * factor min 1.0)

  def opacify(factor: Double) = this match {
    case rgba: Color.RGBA ⇒ rgba.copy(a = rgba.a * factor min 1.0)
    case hsla: Color.HSLA ⇒ hsla.copy(a = hsla.a * factor min 1.0)
  }

  /** Produces a string like #fe76a1 */
  def toRgbString = {
    val rgba = toRgba
    f"#${(rgba.r * 255).toInt}%02x${(rgba.g * 255).toInt}%02x${(rgba.b * 255).toInt}%02x"
  }

  /** Produces a string like #fe76a133 (including the alpha value) */
  def toRgbaString = f"$toRgbString${(a * 255).toInt}%02x"
}

object Color {
  private[geometry] val rgbaComponents = Iso[RGBA, Seq[Double]] { color ⇒
    Seq(color.r, color.g, color.b, color.a)
  } {
    case Seq(r, g, b, a) ⇒ RGBA(r, g, b, a)
  }

  private[geometry] val hslaComponents = Iso[HSLA, Seq[Double]] { color ⇒
    Seq(color.h, color.s, color.l, color.a)
  } {
    case Seq(h, s, l, a) ⇒ HSLA(h, s, l, a)
  }

  private val color2rgba = Iso[Color, RGBA](_.toRgba)(identity)

  case class RGBA(r: Double, g: Double, b: Double, a: Double) extends Color {
    def toRgba = this
    def toHsla = {
      val max = r max g max b
      val min = r min g min b
      val l = (max + min) / 2
      if (max == min) HSLA(0, 0, l, a) else {
        val d = max - min
        val s = if (l > 0.5) d / (2 - max - min) else d / (max + min)
        val h = max match {
          case `r` ⇒ ((g - b) / d + 6) % 6
          case `g` ⇒ (b - r) / d + 2
          case `b` ⇒ (r - g) / d + 4
        }
        HSLA(h / 6, s, l, a)
      }
    }
  }

  case class HSLA(h: Double, s: Double, l: Double, a: Double) extends Color {
    def toHsla = this
    def toRgba = {
      if (s == 0) RGBA(l, l, l, a) else {
        def hue2rgb(p: Double, q: Double, t: Double) = {
          if (t < 1/6.0) p + (q - p) * 6 * t
          else if (t < 1/2.0) q
          else if (t < 2/3.0) p + (q - p) * (2/3.0 - t) * 6
          else p
        }
        val q = if (l < 0.5) l * (1 + s) else l + s - l * s
        val p = 2 * l - q
        RGBA(
          hue2rgb(p, q, (h + 1/3.0) % 1),
          hue2rgb(p, q, h),
          hue2rgb(p, q, (h + 2/3.0) % 1),
          a
        )
      }
    }
  }

  /** Parse a color from an RGB(A) string. If the alpha component is missing, `defaultAlpha` is used */
  def fromRgbaString(string: String, defaultAlpha: Double = 1.0) =
    rgbaParser(defaultAlpha).parse(string).get.value

  private def rgbaParser(defaultAlpha: Double) = {
    import fastparse.all._

    val x = CharIn(('0' to '9') ++ ('a' to 'f'))
    val component = P(x ~ x).!.map(n ⇒ java.lang.Long.parseLong(n, 16) / 255.0)
    P("#" ~ component ~ component ~ component ~ component.?) map {
      case (r, g, b, a) ⇒ RGBA(r, g, b, a.getOrElse(defaultAlpha))
    }
  }

  /** An isomorphism between RGBA color strings and [[Color]] */
  val rgbaStringIso: Iso[String, Color] =
    Iso[String, Color](fromRgbaString(_))(_.toRgbaString)

  /** Interpolate colors by interpolating the RGBA components */
  val interpolation = (color2rgba composeIso rgbaComponents).asLens
    .interpolateEachWith(Interpolation.double)

  /** Mix the provided colors together by averaging their RGBA components */
  def mix(colors: Seq[Color]) = rgbaComponents.reverseGet {
    colors.map(_.toRgba).map(rgbaComponents.get).transpose.map(values ⇒ values.sum / values.length)
  }
}
