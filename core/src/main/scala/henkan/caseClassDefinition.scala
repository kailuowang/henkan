package henkan

import shapeless.labelled.FieldType
import shapeless.ops.hlist._
import shapeless._

sealed trait FieldDefinition[K, V]

trait FieldWithoutDefault[K, V] extends FieldDefinition[K, V]

case class FieldWithDefault[K, V](defaultValue: V) extends FieldDefinition[K, V]

object FieldDefinition {

}

trait CaseClassDefinition[T] {
  type Repr <: HList

  type FieldDefinitions <: HList

  def gen: LabelledGeneric.Aux[T, Repr]

  def fields: FieldDefinitions
}

object CaseClassDefinition {

  type FieldKV[K, +V] = K with ValueTag[K, V]
  trait ValueTag[K, +V]

  trait FieldKVs[L <: HList] extends DepFn0 with Serializable { type Out <: HList }

  object FieldKVs {

    type Aux[L <: HList, Out0 <: HList] = FieldKVs[L] { type Out = Out0 }

    implicit def hnilFieldKV[L <: HNil]: Aux[L, HNil] =
      new FieldKVs[L] {
        type Out = HNil
        def apply(): Out = HNil
      }

    implicit def hlistFieldKVs[K, V, T <: HList](implicit
      wk: Witness.Aux[K],
      kt: FieldKVs[T]): Aux[FieldType[K, V] :: T, FieldKV[K, V] :: kt.Out] =
      new FieldKVs[FieldType[K, V] :: T] {
        type Out = FieldKV[K, V] :: kt.Out
        def apply(): Out = wk.value.asInstanceOf[FieldKV[K, V]] :: kt()
      }
  }

  type Aux[T, Repr0, FieldDefinitions0] = CaseClassDefinition[T] {
    type Repr = Repr0
    type FieldDefinitions = FieldDefinitions0
  }

  private object toFieldDefinition extends Poly2 {
    implicit def withDefault[K, V](implicit k: Witness.Aux[K]) =
      at[FieldKV[K, V], Some[V]] { (_: FieldKV[K, V], defaultValue: Some[V]) ⇒
        FieldWithDefault[K, V](defaultValue.get)
      }

    implicit def withoutDefault[K <: Symbol, V](implicit k: Witness.Aux[K]) =
      at[FieldKV[K, V], None.type] { (_: FieldKV[K, V], _: None.type) ⇒
        new FieldWithoutDefault[K, V] {}
      }
  }

  implicit def mkCaseClassDefinition[T, Repr1 <: HList, KVs <: HList, Defaults <: HList](
    implicit
    lgen: LabelledGeneric.Aux[T, Repr1],
    kvs: FieldKVs.Aux[Repr1, KVs],
    defaults: Default.Aux[T, Defaults],
    zip: ZipWith[KVs, Defaults, toFieldDefinition.type]
  ): Aux[T, Repr1, zip.Out] = new CaseClassDefinition[T] {
    type Repr = Repr1
    type FieldDefinitions = zip.Out
    def gen: LabelledGeneric.Aux[T, Repr] = lgen
    def fields: FieldDefinitions = zip(kvs(), defaults())
  }

  def apply[T](implicit caseClassDefinition: CaseClassDefinition[T]) = caseClassDefinition
}

