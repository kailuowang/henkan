package henkan.optional

import cats.{Traverse}
import cats.data._
import shapeless.labelled._
import shapeless._
import cats.syntax.all._
import shapeless.ops.record.Selector

import ValidateFromOptional.Result

import scala.annotation.implicitNotFound

@implicitNotFound("Cannot build validate function from ${From} to ${To}, possibly due to missing fields in ${From} or missing cats instances (`Traverse` instances are needed to convert fields in containers)")
trait ValidateFromOptional[From, To] {
  def apply(from: From): Result[To]
}

object ValidateFromOptional extends MkValidateFromOptional with ValidateFromOptionalSyntax {
  type Result[T] = ValidatedNel[RequiredFieldMissing, T]

}

trait ValidateFromOptionalSyntax {
  final class validatePartial[From] private[optional] (from: From) {
    def to[To](implicit c: ValidateFromOptional[From, To]) = c(from)
  }

  def validate[From](f: From) = new validatePartial(f)
}

case class RequiredFieldMissing(fieldName: String)

trait MkValidateFromOptional extends MkValidateFromOptional0 {

  implicit def mkGenValidateFromOptional[From, To, FL <: HList, TL <: HList](
    implicit
    genFrom: LabelledGeneric.Aux[From, FL],
    genTo: LabelledGeneric.Aux[To, TL],
    convertHList: Lazy[ValidateFromOptional[FL, TL]]
  ) = new ValidateFromOptional[From, To] {
    def apply(from: From): ValidatedNel[RequiredFieldMissing, To] = {
      convertHList.value(genFrom.to(from)).map(genTo.from)
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
      (headConvert(from), tailConvert(from)).mapN(_ :: _)
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

  implicit def mkFromIdentity[V]: ValidateFromOptional[V, V] = new ValidateFromOptional[V, V] {
    def apply(from: V): Result[V] = {
      Validated.Valid(from)
    }
  }
}

trait MkValidateFromOptional2 extends MkValidateFromOptional3 {
  implicit def mkSingleTraverseValidateFromOptional[TV, V, F[_]](
    implicit
    F: Traverse[F],
    c: Lazy[ValidateFromOptional[V, TV]]
  ): ValidateFromOptional[F[V], F[TV]] = new ValidateFromOptional[F[V], F[TV]] {
    def apply(from: F[V]): Result[F[TV]] =
      F.traverse(from)(vv ⇒ c.value(vv))
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

  implicit def mkSingleRecursiveValidateDirectFromOptional[FL <: HList, K <: Symbol: Witness.Aux, TV, FV](
    implicit
    selector: Lazy[Selector.Aux[FL, K, FV]],
    c: Lazy[ValidateFromOptional[FV, TV]]
  ): ValidateFromOptional[FL, FieldType[K, TV]] = new ValidateFromOptional[FL, FieldType[K, TV]] {
    def apply(from: FL): Result[FieldType[K, TV]] = {
      c.value(selector.value(from)).map(field[K](_))
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
