package henkan

import cats._
import cats.data.Kleisli
import cats.sequence._
import henkan.FieldReader.FieldName
import shapeless._
import shapeless.labelled._
import shapeless.ops.hlist.Mapper

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

  trait FieldExtractor[F[_], S, T] {
    def apply(fieldName: FieldName): Kleisli[F, S, T]
  }

  object FieldExtractor {

    implicit def mkFieldExtractor[F[_], S, T](
      implicit
      fr: FieldReader[F, S, T]
    ): FieldExtractor[F, S, T] = new FieldExtractor[F, S, T] {
      def apply(fieldName: FieldName) = fr.apply(fieldName)
    }

    implicit def recursiveFieldExtractor[F[_], S, T](
      implicit
      ex: Extractor[F, S, T],
      un: Unapply.Aux1[FlatMap, F[S], F, S],
      decomposer: Decomposer[F, S]
    ): FieldExtractor[F, S, T] = new FieldExtractor[F, S, T] {

      def apply(fieldName: FieldName): Kleisli[F, S, T] = Kleisli(
        s ⇒ {
          val subcomp = decomposer(s, fieldName)
          un.TC.flatMap(subcomp)(ex.extract)
        }
      )
    }
  }

  object fieldExtractorMapper extends Poly1 {

    implicit def caseField[K <: Symbol, V, S, F[_]](
      implicit
      fr: FieldExtractor[F, S, V]
    ) = at[FieldDef[K, V]] { f ⇒
      import labelled.field
      field[K](fr(f.name))
    }

  }

  implicit def mkExtractor[F[_], S, T, Repr <: HList, FDs <: HList, ReprKleisli <: HList](
    implicit
    gen: LabelledGeneric.Aux[T, Repr],
    fds: FieldDefs.Aux[Repr, FDs],
    mapper: Mapper.Aux[fieldExtractorMapper.type, FDs, ReprKleisli],
    sequencer: RecordSequencer.Aux[ReprKleisli, Kleisli[F, S, Repr]],
    un: Unapply.Aux1[Functor, F[Repr], F, Repr]
  ): Extractor[F, S, T] = new Extractor[F, S, T] {

    def apply(): Kleisli[F, S, T] = {
      implicit val functor: Functor[F] = un.TC
      sequencer(mapper(fds())).map(gen.from)
    }

  }

  def apply[F[_], S, T](implicit ex: Extractor[F, S, T]): Extractor[F, S, T] = ex

}

trait Decomposer[F[_], S] extends ((S, FieldName) ⇒ F[S])

object Decomposer {
  def apply[F[_], S](f: (S, FieldName) ⇒ F[S]): Decomposer[F, S] = new Decomposer[F, S] {
    def apply(s: S, fieldName: FieldName): F[S] = f(s, fieldName)
  }
}

trait FieldReader[F[_], S, T] extends ((FieldName) ⇒ Kleisli[F, S, T])

object FieldReader {
  type FieldName = String
  def apply[F[_], S, T](f: (S, FieldName) ⇒ F[T]) = new FieldReader[F, S, T] {
    def apply(fieldName: FieldName): Kleisli[F, S, T] =
      Kleisli(s ⇒ f(s, fieldName))
  }
}

trait ExtractorSyntax {

  class extractC[F[_], T]() {
    def apply[S](s: S)(implicit e: Extractor[F, S, T]): F[T] = e.extract(s)
  }

  def extract[F[_], T] = new extractC[F, T]
}
