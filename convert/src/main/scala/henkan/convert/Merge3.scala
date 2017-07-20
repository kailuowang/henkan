
package henkan.convert
import shapeless._, shapeless.ops.record._
import shapeless.labelled._

/**
 * Merge 3 records to the form of `Out`, the sequence represents the priority,
 * fields not present in `Out` are discarded.
 * @tparam A
 * @tparam B
 * @tparam C
 * @tparam Out
 */
@annotation.implicitNotFound("""
    Cannot find Builder
    ${A}, ${B}, ${C} combined must have all the fields ${Out} has """)
trait Merge3[A <: HList, B <: HList, C <: HList, Out] {
  def apply(a: A, b: B, c: C): Out
}

object Merge3 extends MKMerge3

trait MkMerge3_2 {
  implicit def mkCon[A <: HList, B <: HList, C <: HList, K, V, T <: HList](
    implicit
    h: Merge3[A, B, C, FieldType[K, V]],
    t: Merge3[A, B, C, T]
  ): Merge3[A, B, C, FieldType[K, V] :: T] = new Merge3[A, B, C, FieldType[K, V] :: T] {

    override def apply(a: A, b: B, c: C): FieldType[K, V] :: T = h(a, b, c) :: t(a, b, c)
  }
}

trait MKMerge3_1 extends MkMerge3_2 {

  implicit def mkSingleC[A <: HList, B <: HList, C <: HList, K, V](
    implicit
    selector: Selector.Aux[C, K, V]
  ): Merge3[A, B, C, FieldType[K, V]] =
    new Merge3[A, B, C, FieldType[K, V]] {
      override def apply(a: A, b: B, c: C): FieldType[K, V] = field[K](selector(c))
    }
}

trait MKMerge3_0 extends MKMerge3_1 {

  implicit def mkSingleB[A <: HList, B <: HList, C <: HList, K, V](
    implicit
    selector: Selector.Aux[B, K, V]
  ): Merge3[A, B, C, FieldType[K, V]] =
    new Merge3[A, B, C, FieldType[K, V]] {
      override def apply(a: A, b: B, c: C): FieldType[K, V] = field[K](selector(b))
    }
}

trait MKMerge3 extends MKMerge3_0 {

  implicit def mkHNil[A <: HList, B <: HList, C <: HList]: Merge3[A, B, C, HNil] =
    new Merge3[A, B, C, HNil] {
      override def apply(a: A, b: B, c: C): HNil = HNil
    }

  implicit def mkSingleA[A <: HList, B <: HList, C <: HList, K, V](
    implicit
    selector: Selector.Aux[A, K, V]
  ): Merge3[A, B, C, FieldType[K, V]] =
    new Merge3[A, B, C, FieldType[K, V]] {
      override def apply(a: A, b: B, c: C): FieldType[K, V] = field[K](selector(a))
    }
}
