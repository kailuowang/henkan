package henkan

import shapeless.ops.record.RemoveAll
import shapeless.{LabelledGeneric, HList}

import scala.annotation.implicitNotFound

@implicitNotFound("One or more fields in ${Fields} is not in ${T}")
trait CheckFields[Fields <: HList, T]

object CheckFields {
  implicit def check[Fields <: HList, T, Repr <: HList](
    implicit
    gen: LabelledGeneric.Aux[T, Repr],
    s: RemoveAll[Repr, Fields]
  ): CheckFields[Fields, T] = null
}
