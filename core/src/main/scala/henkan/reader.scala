package henkan

import cats.Applicative
import cats.data.Kleisli
import cats.sequence._
import shapeless._
import shapeless.labelled._
import shapeless.ops.hlist.Mapper
import shapeless.ops.record.Values

trait Extractor[F[_], S, T] {
  def apply(): Kleisli[F, S, T]
  def extract(s: S) = apply().run(s)
}

object Extractor {

  type FieldDef[K, +V] = K with ValueTag[K, V]
  trait ValueTag[K, +V]

  trait FieldDefs[L <: HList] extends DepFn0 with Serializable { type Out <: HList }

  object FieldDefs {

    def apply[L <: HList](implicit fds: FieldDefs[L]): Aux[L, fds.Out] = fds

    type Aux[L <: HList, Out0 <: HList] = FieldDefs[L] { type Out = Out0 }

    implicit def hnilFieldDefs[L <: HNil]: Aux[L, HNil] =
      new FieldDefs[L] {
        type Out = HNil
        def apply(): Out = HNil
      }

    implicit def hlistFieldDefs[K, V, T <: HList](implicit
      wk: Witness.Aux[K],
      kt: FieldDefs[T]): Aux[FieldType[K, V] :: T, FieldDef[K, V] :: kt.Out] =
      new FieldDefs[FieldType[K, V] :: T] {
        type Out = FieldDef[K, V] :: kt.Out
        def apply(): Out = wk.value.asInstanceOf[FieldDef[K, V]] :: kt()
      }
  }

  object fieldExtractorMapper extends Poly1 {
    implicit def caseField[K <: Symbol, V, S, F[_]](implicit fr: FieldReader[F, S, V]) = at[FieldDef[K, V]] { f ⇒
      import labelled.field
      field[K](fr(f.name))
    }
  }

  implicit def mkExtractor[F[_]: Applicative, S, T, Repr <: HList, FDs <: HList, ReprKleisli <: HList](
    implicit
    gen: LabelledGeneric.Aux[T, Repr],
    fds: FieldDefs.Aux[Repr, FDs],
    mapper: Mapper.Aux[fieldExtractorMapper.type, FDs, ReprKleisli],
    sequencer: RecordSequencer.Aux[ReprKleisli, Kleisli[F, S, Repr]]

  ) = new Extractor[F, S, T] {

    def apply(): Kleisli[F, S, T] = {
      sequencer(mapper(fds())).map(gen.from)
    }

  }

  class extractC[F[_], T]() {
    def apply[S](s: S)(implicit e: Extractor[F, S, T]): F[T] = e.extract(s)
  }

  def extract[F[_], T] = new extractC[F, T]

}

trait FieldReader[F[_], S, T] {
  import FieldReader.FieldName
  def apply(fieldName: FieldName): Kleisli[F, S, T]
}

object FieldReader {
  type FieldName = String
  def apply[F[_], S, T](f: (S, FieldName) ⇒ F[T]) = new FieldReader[F, S, T] {
    def apply(fieldName: FieldName): Kleisli[F, S, T] =
      Kleisli(s ⇒ f(s, fieldName))
  }
}

