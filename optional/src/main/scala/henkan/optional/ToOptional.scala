package henkan.optional

import cats.Functor
import shapeless.labelled._
import shapeless.ops.record.Selector
import shapeless._
import cats.syntax.functor._
import scala.annotation.implicitNotFound

@implicitNotFound("Cannot build toOptional conversion from ${From} to ${To}, possibly due to missing cats instances (`Functor` instances are needed to convert fields in containers)")
trait ToOptional[From, To] {
  def apply(from: From): To

}

object ToOptional extends ToOptionalSyntax with MkToOptional

trait ToOptionalSyntax {
  final class fromPartial[From] private[optional] (f: From) {
    def toOptional[To](implicit t: ToOptional[From, To]) = t(f)
  }

  def from[From](f: From) = new fromPartial(f)
}

trait MkToOptional extends MkToOptional0 {

  implicit def mkGenToOptional[From, To, FL <: HList, TL <: HList](
    implicit
    genFrom: LabelledGeneric.Aux[From, FL],
    genTo: LabelledGeneric.Aux[To, TL],
    convertHList: Lazy[ToOptional[FL, TL]]
  ) = new ToOptional[From, To] {
    def apply(from: From): To = {
      genTo.from(convertHList.value(genFrom.to(from)))
    }
  }

}

trait MkToOptional0 extends MkToOptional1 {
  implicit def mkNilToOptional[FL <: HList]: ToOptional[FL, HNil] = new ToOptional[FL, HNil] {
    def apply(from: FL): HNil = HNil
  }

  implicit def mkConToOptional[FL <: HList, K, V, FV, TL <: HList](
    implicit
    tailConvert: ToOptional[FL, TL],
    headConvert: ToOptional[FL, FieldType[K, FV]]
  ): ToOptional[FL, FieldType[K, FV] :: TL] = new ToOptional[FL, FieldType[K, FV] :: TL] {

    def apply(from: FL): FieldType[K, FV] :: TL = {
      headConvert(from) :: tailConvert(from)
    }
  }

  implicit def mkSingleRequiredToRequired[FL <: HList, K, V](
    implicit
    selector: Selector.Aux[FL, K, V]
  ): ToOptional[FL, FieldType[K, V]] = new ToOptional[FL, FieldType[K, V]] {
    def apply(from: FL): FieldType[K, V] =
      field[K](selector(from))
  }
}

trait MkToOptional1 extends MkToOptional2 {
  implicit def mkSingleOptionalToOptional[FL <: HList, K, V](
    implicit
    selector: Selector.Aux[FL, K, Option[V]]
  ): ToOptional[FL, FieldType[K, Option[V]]] = new ToOptional[FL, FieldType[K, Option[V]]] {
    def apply(from: FL): FieldType[K, Option[V]] =
      field[K](selector(from))

  }

}

trait MkToOptional2 extends MkToOptional3 {

  implicit def mkSingleTraverseToOptional[FL <: HList, K <: Symbol, TV, FV, F[_]](
    implicit
    F: Functor[F],
    selector: Lazy[Selector.Aux[FL, K, F[FV]]],
    k: Witness.Aux[K],
    c: Lazy[ToOptional[FV, TV]]
  ): ToOptional[FL, FieldType[K, F[TV]]] = new ToOptional[FL, FieldType[K, F[TV]]] {
    def apply(from: FL): FieldType[K, F[TV]] = {
      val v: F[FV] = selector.value(from)
      field[K](v.map(c.value.apply))
    }
  }
}

trait MkToOptional3 extends MkToOptional4 {
  implicit def mkSingleRecursiveToOptional[FL <: HList, K <: Symbol, TV, FV](
    implicit
    selector: Lazy[Selector.Aux[FL, K, FV]],
    k: Witness.Aux[K],
    c: Lazy[ToOptional[FV, TV]]
  ): ToOptional[FL, FieldType[K, Option[TV]]] = new ToOptional[FL, FieldType[K, Option[TV]]] {
    def apply(from: FL): FieldType[K, Option[TV]] = {
      val v: FV = selector.value(from)
      field[K](Some(c.value(v)))
    }
  }

  implicit def mkSingleRecursiveFieldToOptional[FL <: HList, K <: Symbol, TV, FV](
    implicit
    selector: Lazy[Selector.Aux[FL, K, FV]],
    k: Witness.Aux[K],
    c: Lazy[ToOptional[FL, FieldType[K, TV]]]
  ): ToOptional[FL, FieldType[K, Option[TV]]] = mapField(c.value)(Option(_))
}

trait MkToOptional4 extends MkToOption5 {
  implicit def mkSingleToOptional[FL <: HList, K <: Symbol, V](
    implicit
    selector: Selector.Aux[FL, K, V],
    k: Witness.Aux[K]
  ): ToOptional[FL, FieldType[K, Option[V]]] = new ToOptional[FL, FieldType[K, Option[V]]] {
    def apply(from: FL): FieldType[K, Option[V]] = {
      field[K](Some(selector(from)))
    }
  }
}

trait MkToOption5 {
  implicit def mkSingleMissingToOptional[FL <: HList, K, V]: ToOptional[FL, FieldType[K, Option[V]]] = new ToOptional[FL, FieldType[K, Option[V]]] {
    def apply(from: FL): FieldType[K, Option[V]] =
      field[K](None)
  }

  protected def mapField[FL <: HList, K, A, B](o: ToOptional[FL, FieldType[K, A]])(f: A ⇒ B): ToOptional[FL, FieldType[K, B]] = new ToOptional[FL, FieldType[K, B]] {
    override def apply(from: FL): FieldType[K, B] = field[K](f(o(from)))
  }

}
