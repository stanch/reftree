package reftree.geometry

import monocle.Iso

object Color {
  val rgbaComponents = Iso[RGBA, Seq[Double]] { color ⇒
    Seq(color.r, color.g, color.b, color.a)
  } {
    case Seq(r, g, b, a) ⇒ RGBA(r, g, b, a)
  }

  val hslaComponents = Iso[HSLA, Seq[Double]] { color ⇒
    Seq(color.h, color.s, color.l, color.a)
  } {
    case Seq(h, s, l, a) ⇒ HSLA(h, s, l, a)
  }

  case class RGBA(r: Double, g: Double, b: Double, a: Double) {
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

    override def toString = f"#${(r * 255).toInt}%02x${(g * 255).toInt}%02x${(b * 255).toInt}%02x"
  }

  object RGBA {
    def fromString(string: String, a: Double = 1.0) = parser(a).parse(string).get.value

    private def parser(a: Double) = {
      import fastparse.all._

      val x = CharIn(('0' to '9') ++ ('a' to 'f'))
      val component = P(x ~ x).!.map(n ⇒ java.lang.Long.parseLong(n, 16) / 255.0)
      P("#" ~ component ~ component ~ component) map {
        case (r, g, b) ⇒ RGBA(r, g, b, a)
      }
    }

    val interpolation = Interpolation.seq(Interpolation.double).lensLeft(rgbaComponents.asLens)
  }

  case class HSLA(h: Double, s: Double, l: Double, a: Double) {
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
}
