package henkan.optional

import cats.{Traverse, Functor}
import cats.data._
import shapeless.labelled._
import shapeless._
import cats.syntax.all._
import shapeless.ops.record.Selector

import ConvertFromOptional.Result

import scala.annotation.implicitNotFound

trait ConvertToOptional[From, To] {
  def apply(from: From): To
}

@implicitNotFound("Cannot build conversion from ${From} to ${To}, possibly due to missing fields in ${From}")
trait ConvertFromOptional[From, To] {
  def apply(from: From): Result[To]
}

trait MkConvertFromOptional4 {
  def missingField[T, K <: Symbol](implicit k: Witness.Aux[K]): Result[T] = Validated.invalidNel(RequiredFieldMissing(k.value.name))

  implicit def mkSingleConvertFromOptional[FL <: HList, K <: Symbol, V](
    implicit
    selector: Selector.Aux[FL, K, Option[V]],
    k: Witness.Aux[K]
  ): ConvertFromOptional[FL, FieldType[K, V]] = new ConvertFromOptional[FL, FieldType[K, V]] {
    def apply(from: FL): Result[FieldType[K, V]] = {
      selector(from).fold[Result[FieldType[K, V]]](missingField)(v ⇒ Validated.Valid(field[K](v)))
    }
  }
}

trait MkConvertFromOptional3 extends MkConvertFromOptional4 {
  implicit def mkSingleRecursiveConvertFromOptional[FL <: HList, K <: Symbol, TV, FV](
    implicit
    selector: Lazy[Selector.Aux[FL, K, Option[FV]]],
    k: Witness.Aux[K],
    c: Lazy[ConvertFromOptional[FV, TV]]
  ): ConvertFromOptional[FL, FieldType[K, TV]] = new ConvertFromOptional[FL, FieldType[K, TV]] {
    def apply(from: FL): Result[FieldType[K, TV]] = {
      selector.value(from).fold[Result[FieldType[K, TV]]](missingField) { v ⇒
        c.value(v).map(field[K](_))
      }
    }
  }
}

trait MkConvertFromOptional2 extends MkConvertFromOptional3 {
  implicit def mkSingleTraverseConvertFromOptional[FL <: HList, K <: Symbol, TV, FV, F[_]](
    implicit
    F: Traverse[F],
    selector: Lazy[Selector.Aux[FL, K, Option[F[FV]]]],
    k: Witness.Aux[K],
    c: Lazy[ConvertFromOptional[FV, TV]]
  ): ConvertFromOptional[FL, FieldType[K, F[TV]]] = new ConvertFromOptional[FL, FieldType[K, F[TV]]] {
    def apply(from: FL): Result[FieldType[K, F[TV]]] = {
      selector.value(from).fold[Result[FieldType[K, F[TV]]]](missingField) { v ⇒
        F.traverse(v)(vv ⇒ c.value(vv)).map(field[K](_))
      }
    }
  }
}

trait MkConvertFromOptional1 extends MkConvertFromOptional2 {
  implicit def mkSingleOptionalConvertFromOptional[FL <: HList, K, V](
    implicit
    selector: Selector.Aux[FL, K, Option[V]]
  ): ConvertFromOptional[FL, FieldType[K, Option[V]]] = new ConvertFromOptional[FL, FieldType[K, Option[V]]] {
    def apply(from: FL): Result[FieldType[K, Option[V]]] = {
      Validated.Valid(field[K](selector(from)))
    }
  }
}

trait MkConvertFromOptional0 extends MkConvertFromOptional1 {
  implicit def mkNilConvertFromOptional[FL <: HList]: ConvertFromOptional[FL, HNil] = new ConvertFromOptional[FL, HNil] {
    def apply(from: FL): Result[HNil] = Validated.Valid(HNil)
  }

  implicit def mkConConvertFromOptional[FL <: HList, K, V, TL <: HList](
    implicit
    tailConvert: ConvertFromOptional[FL, TL],
    headConvert: ConvertFromOptional[FL, FieldType[K, V]]
  ): ConvertFromOptional[FL, FieldType[K, V] :: TL] = new ConvertFromOptional[FL, FieldType[K, V] :: TL] {

    def apply(from: FL): Result[FieldType[K, V] :: TL] = {
      (headConvert(from), tailConvert(from)).map2(_ :: _)
    }
  }

}

object ConvertFromOptional extends MkConvertFromOptional0 {
  type Result[T] = ValidatedNel[RequiredFieldMissing, T]

  implicit def mkGenConvertFromOptional[From, To, FL <: HList, TL <: HList](
    implicit
    genFrom: LabelledGeneric.Aux[From, FL],
    genTo: LabelledGeneric.Aux[To, TL],
    convertHList: ConvertFromOptional[FL, TL]
  ) = new ConvertFromOptional[From, To] {
    def apply(from: From): ValidatedNel[RequiredFieldMissing, To] = {
      convertHList(genFrom.to(from)).map(genTo.from)
    }
  }

  case class validate[From](from: From) {
    def to[To](implicit c: ConvertFromOptional[From, To]) = c(from)
  }
}

case class RequiredFieldMissing(fieldName: String)

