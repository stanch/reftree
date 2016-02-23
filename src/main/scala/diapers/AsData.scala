package diapers

import shapeless.{Lazy, HList, HNil, Generic}

sealed trait Data

object Data {
  case class Val(value: AnyVal) extends Data

  case class Ref(name: String, id: String, data: Seq[Data]) extends Data
  object Ref {
    def apply(value: AnyRef, data: Seq[Data]): Ref =
      Ref(
        value.getClass.getSimpleName,
        s"${value.getClass.getName}-${System.identityHashCode(value)}",
        data
      )
  }
}

trait AsData[-A] {
  def asData(value: A): Data
}

object AsData {
  implicit class AsDataOps[A: AsData](value: A) {
    def asData = implicitly[AsData[A]].asData(value)
  }

  implicit def `AnyVal as Data`: AsData[AnyVal] = new AsData[AnyVal] {
    def asData(value: AnyVal) = Data.Val(value)
  }

  implicit def `String as Data`: AsData[String] = new AsData[String] {
    def asData(value: String) = Data.Ref(value, value.map(Data.Val))
  }

  implicit def `List as Data`[A: AsData]: AsData[List[A]] = new AsData[List[A]] {
    def asData(value: List[A]): Data = value match {
      case head :: tail ⇒ Data.Ref(value, Seq(head.asData, asData(tail))).copy(name = "Cons")
      case Nil ⇒ Data.Ref(Nil, Seq.empty).copy(name = "Nil")
    }
  }

  import shapeless.::

  implicit val `HNil as Data`: AsData[HNil] = new AsData[HNil] {
    def asData(value: HNil) = Data.Ref(value, Seq.empty)
  }

  implicit def `HCons as Data`[H: AsData, T <: HList: AsData]: AsData[H :: T] = new AsData[H :: T] {
    def asData(value: H :: T): Data = Data.Ref(value, value.head.asData +: (value.tail.asData match {
      case Data.Ref(_, _, data) ⇒ data
      case x ⇒ Seq(x)
    }))
  }

  implicit def `Generic as Data`[A <: AnyRef, L <: HList](
    implicit generic: Generic.Aux[A, L], hListAsData: Lazy[AsData[L]]
  ): AsData[A] = new AsData[A] {
    def asData(value: A) = hListAsData.value.asData(generic.to(value)) match {
      case product: Data.Ref ⇒ Data.Ref(value, product.data)
      case x ⇒ x
    }
  }
}
