
package henkan.convert
import shapeless._, shapeless.ops.record._
import shapeless.labelled._

@annotation.implicitNotFound("""
    Cannot find Builder
    ${A}, ${B}, ${C} combined must have all the fields ${Out} has """)
trait BuildFrom[A <: HList, B <: HList, C <: HList, Out] {
  def apply(a: A, b: B, c: C): Out
}

object BuildFrom extends MKBuildFrom

trait MKBuildFrom2 {
  implicit def mkCon[A <: HList, B <: HList, C <: HList, K, V, T <: HList](
    implicit
    h: BuildFrom[A, B, C, FieldType[K, V]],
    t: BuildFrom[A, B, C, T]
  ): BuildFrom[A, B, C, FieldType[K, V] :: T] = new BuildFrom[A, B, C, FieldType[K, V] :: T] {

    override def apply(a: A, b: B, c: C): FieldType[K, V] :: T = h(a, b, c) :: t(a, b, c)
  }
}

trait MKBuildFrom1 extends MKBuildFrom2 {

  implicit def mkSingleC[A <: HList, B <: HList, C <: HList, K, V](
    implicit
    selector: Selector.Aux[C, K, V]
  ): BuildFrom[A, B, C, FieldType[K, V]] =
    new BuildFrom[A, B, C, FieldType[K, V]] {
      override def apply(a: A, b: B, c: C): FieldType[K, V] = field[K](selector(c))
    }
}

trait MKBuildFrom0 extends MKBuildFrom1 {

  implicit def mkSingleB[A <: HList, B <: HList, C <: HList, K, V](
    implicit
    selector: Selector.Aux[B, K, V]
  ): BuildFrom[A, B, C, FieldType[K, V]] =
    new BuildFrom[A, B, C, FieldType[K, V]] {
      override def apply(a: A, b: B, c: C): FieldType[K, V] = field[K](selector(b))
    }
}

trait MKBuildFrom extends MKBuildFrom0 {

  implicit def mkHNil[A <: HList, B <: HList, C <: HList]: BuildFrom[A, B, C, HNil] =
    new BuildFrom[A, B, C, HNil] {
      override def apply(a: A, b: B, c: C): HNil = HNil
    }

  implicit def mkSingleA[A <: HList, B <: HList, C <: HList, K, V](
    implicit
    selector: Selector.Aux[A, K, V]
  ): BuildFrom[A, B, C, FieldType[K, V]] =
    new BuildFrom[A, B, C, FieldType[K, V]] {
      override def apply(a: A, b: B, c: C): FieldType[K, V] = field[K](selector(a))
    }
}
