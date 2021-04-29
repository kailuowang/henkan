package henkan.convert

import shapeless._
import shapeless.labelled.FieldType
import shapeless.ops.record._

import scala.annotation.implicitNotFound

@implicitNotFound("One or more fields in ${SL} is not in ${L}")
trait ContainsAll[L <: HList, SL]

object ContainsAll extends MkContainsAll0

abstract class MkContainsAll0 {
  implicit def AllContainsNil[L <: HList]: ContainsAll[L, HNil] = new ContainsAll[L, HNil] {}

  @annotation.nowarn
  implicit def ContainsCon[L <: HList, K, V, ST <: HList](
    implicit
    selector: Selector.Aux[L, K, V],
    CAT: ContainsAll[L, ST]
  ): ContainsAll[L, FieldType[K, V] :: ST] = new ContainsAll[L, FieldType[K, V] :: ST] {}
}
