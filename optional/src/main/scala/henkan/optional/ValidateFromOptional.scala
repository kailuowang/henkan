package henkan.optional

import cats.{Traverse, Functor}
import cats.data._
import shapeless.labelled._
import shapeless._
import cats.syntax.all._
import shapeless.ops.record.Selector

import ValidateFromOptional.Result

import scala.annotation.implicitNotFound

@implicitNotFound("Cannot build conversion from ${From} to ${To}, possibly due to missing fields in ${From}")
trait ValidateFromOptional[From, To] {
  def apply(from: From): Result[To]
}

object ValidateFromOptional extends MkValidateFromOptional with ValidateFromOptionalSyntax {
  type Result[T] = ValidatedNel[RequiredFieldMissing, T]

}

trait ValidateFromOptionalSyntax {
  case class validate[From](from: From) {
    def to[To](implicit c: ValidateFromOptional[From, To]) = c(from)
  }

}

case class RequiredFieldMissing(fieldName: String)

trait MkValidateFromOptional extends MkValidateFromOptional0 {

  implicit def mkGenValidateFromOptional[From, To, FL <: HList, TL <: HList](
    implicit
    genFrom: LabelledGeneric.Aux[From, FL],
    genTo: LabelledGeneric.Aux[To, TL],
    convertHList: ValidateFromOptional[FL, TL]
  ) = new ValidateFromOptional[From, To] {
    def apply(from: From): ValidatedNel[RequiredFieldMissing, To] = {
      convertHList(genFrom.to(from)).map(genTo.from)
    }
  }

}

trait MkValidateFromOptional0 extends MkValidateFromOptional1 {
  implicit def mkNilValidateFromOptional[FL <: HList]: ValidateFromOptional[FL, HNil] = new ValidateFromOptional[FL, HNil] {
    def apply(from: FL): Result[HNil] = Validated.Valid(HNil)
  }

  implicit def mkConValidateFromOptional[FL <: HList, K, V, TL <: HList](
    implicit
    tailConvert: ValidateFromOptional[FL, TL],
    headConvert: ValidateFromOptional[FL, FieldType[K, V]]
  ): ValidateFromOptional[FL, FieldType[K, V] :: TL] = new ValidateFromOptional[FL, FieldType[K, V] :: TL] {

    def apply(from: FL): Result[FieldType[K, V] :: TL] = {
      (headConvert(from), tailConvert(from)).map2(_ :: _)
    }
  }
}

trait MkValidateFromOptional1 extends MkValidateFromOptional2 {
  implicit def mkSingleOptionalValidateFromOptional[FL <: HList, K, V](
    implicit
    selector: Selector.Aux[FL, K, Option[V]]
  ): ValidateFromOptional[FL, FieldType[K, Option[V]]] = new ValidateFromOptional[FL, FieldType[K, Option[V]]] {
    def apply(from: FL): Result[FieldType[K, Option[V]]] = {
      Validated.Valid(field[K](selector(from)))
    }
  }
}

trait MkValidateFromOptional2 extends MkValidateFromOptional3 {
  implicit def mkSingleTraverseValidateFromOptional[FL <: HList, K <: Symbol, TV, FV, F[_]](
    implicit
    F: Traverse[F],
    selector: Lazy[Selector.Aux[FL, K, Option[F[FV]]]],
    k: Witness.Aux[K],
    c: Lazy[ValidateFromOptional[FV, TV]]
  ): ValidateFromOptional[FL, FieldType[K, F[TV]]] = new ValidateFromOptional[FL, FieldType[K, F[TV]]] {
    def apply(from: FL): Result[FieldType[K, F[TV]]] = {
      selector.value(from).fold[Result[FieldType[K, F[TV]]]](missingField) { v ⇒
        F.traverse(v)(vv ⇒ c.value(vv)).map(field[K](_))
      }
    }
  }
}

trait MkValidateFromOptional3 extends MkValidateFromOptional4 {
  implicit def mkSingleRecursiveValidateFromOptional[FL <: HList, K <: Symbol, TV, FV](
    implicit
    selector: Lazy[Selector.Aux[FL, K, Option[FV]]],
    k: Witness.Aux[K],
    c: Lazy[ValidateFromOptional[FV, TV]]
  ): ValidateFromOptional[FL, FieldType[K, TV]] = new ValidateFromOptional[FL, FieldType[K, TV]] {
    def apply(from: FL): Result[FieldType[K, TV]] = {
      selector.value(from).fold[Result[FieldType[K, TV]]](missingField) { v ⇒
        c.value(v).map(field[K](_))
      }
    }
  }
}

trait MkValidateFromOptional4 {
  def missingField[T, K <: Symbol](implicit k: Witness.Aux[K]): Result[T] = Validated.invalidNel(RequiredFieldMissing(k.value.name))

  implicit def mkSingleValidateFromOptional[FL <: HList, K <: Symbol, V](
    implicit
    selector: Selector.Aux[FL, K, Option[V]],
    k: Witness.Aux[K]
  ): ValidateFromOptional[FL, FieldType[K, V]] = new ValidateFromOptional[FL, FieldType[K, V]] {
    def apply(from: FL): Result[FieldType[K, V]] = {
      selector(from).fold[Result[FieldType[K, V]]](missingField)(v ⇒ Validated.Valid(field[K](v)))
    }
  }
}
