package henkan.convert

import shapeless.ops.hlist.Union
import shapeless.{LabelledGeneric, HList}

import scala.annotation.implicitNotFound

@implicitNotFound("One or more fields in ${Fields} is not in ${T}")
trait CheckFields[Fields <: HList, T]

object CheckFields {
  implicit def check[Fields <: HList, T, Repr <: HList](
    implicit
    gen: LabelledGeneric.Aux[T, Repr],
    s: Union.Aux[Repr, Fields, Repr]
  ): CheckFields[Fields, T] = null
}
