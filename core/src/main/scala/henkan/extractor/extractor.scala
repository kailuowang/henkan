package henkan.extractor

import alleycats.{Pure, EmptyK}
import cats._
import cats.data.Kleisli
import cats.sequence._
import henkan.{CaseClassDefinition, FieldWithoutDefault, FieldWithDefault, FieldName}
import shapeless._
import shapeless.labelled._
import shapeless.ops.hlist.Mapper

import scala.annotation.implicitNotFound
import scala.annotation.unchecked.{uncheckedVariance ⇒ uV}
import scala.collection.GenTraversableOnce
import scala.collection.generic.CanBuildFrom

@implicitNotFound(
  """
    For all fields in ${T} of type FT, there must be an implicit FieldReader[${F}, ${S}, FT].
    ${F} needs to have instances of cats.FlatMap and cats.Functor.
    If case class with default value is needed, ${F} needs to have instances of alleyCats.EmptyK and cats.Monad
    To extract hierarchical case classes, you need to have an implicit FieldReader[${F}, ${S}, ${S}], that is,
    extract a sub ${S} out of a field of ${S}.
  """
)
trait Extractor[F[_], S, T] {
  def apply(): Kleisli[F, S, T]
  def extract(s: S) = apply().run(s)
}

object Extractor {

  trait FieldExtractor[F[_], S, T] {
    def apply(fieldName: FieldName): Kleisli[F, S, T]
  }

  trait lowPriorityFieldExtractor {
    implicit def recursiveFieldExtractor[F[_], S, T](
      implicit
      ex: Extractor[F, S, T],
      un: Unapply.Aux1[FlatMap, F[S], F, S],
      fr: FieldReader[F, S, S]
    ): FieldExtractor[F, S, T] = FieldExtractor(fr.flatMap(ex.extract))

    implicit def highKindedRecursiveFieldExtractorWithMapK[F[_], S, RG[_], G[_], T](
      implicit
      ex: Extractor[F, S, T],
      fr: FieldReader[F, S, RG[S]],
      unF: Unapply.Aux1[Traverse, G[F[T]], G, F[T]],
      evi: RG[S] <:< GenTraversableOnce[S],
      ff: cats.Unapply.Aux1[Functor, G[S], G, S],
      ua: Unapply.Aux1[Applicative, F[RG[S]], F, RG[S]],
      ffm: Unapply.Aux1[FlatMap, F[RG[S]], F, RG[S]],
      cb: CanBuildFrom[Nothing, S, G[S @uV]]
    ): FieldExtractor[F, S, G[T]] = FieldExtractor(fr.flatMapK[G, RG, S, T](ex.extract))
  }

  object FieldExtractor extends lowPriorityFieldExtractor {

    implicit def apply[F[_], S, T](
      implicit
      fr: FieldReader[F, S, T]
    ): FieldExtractor[F, S, T] = new FieldExtractor[F, S, T] {
      def apply(fieldName: FieldName) = fr.apply(fieldName)
    }

  }

  trait lowPriorityMapper extends Poly1 {
    import labelled.field

    /**
     * Used when EmptyK and Pure instances are not available for F
     * , in which case, default value will be ignored
     */
    implicit def lpCaseFieldWithDefault[K <: Symbol, V, S, F[_]](
      implicit
      fr: FieldExtractor[F, S, V],
      wk: Witness.Aux[K]
    ) = at[FieldWithDefault[K, V]] { f ⇒
      field[K](fr(wk.value.name))
    }
  }

  object fieldExtractorMapper extends lowPriorityMapper {
    import labelled.field

    implicit def caseFieldWithoutDefault[K <: Symbol, V, S, F[_]](
      implicit
      fr: FieldExtractor[F, S, V],
      wk: Witness.Aux[K]
    ) = at[FieldWithoutDefault[K, V]] { f ⇒
      field[K](fr(wk.value.name))
    }

    implicit def caseFieldWithDefault[K <: Symbol, V, S, F[_]](
      implicit
      fr: FieldExtractor[F, S, V],
      wk: Witness.Aux[K],
      ue: Unapply.Aux1[EmptyK, F[V], F, V],
      up: Unapply.Aux1[Pure, F[V], F, V]
    ) = at[FieldWithDefault[K, V]] { f ⇒
      val extracted = fr(wk.value.name)

      field[K](extracted.mapF[F, V] { (fv: F[V]) ⇒
        if (fv == ue.TC.empty[V])
          up.TC.pure(f.defaultValue)
        else
          fv
      })
    }

  }

  implicit def mkExtractor[F[_], S, T, Repr <: HList, FDs <: HList, ReprKleisli <: HList](
    implicit
    ccd: CaseClassDefinition.Aux[T, Repr, FDs],
    mapper: Mapper.Aux[fieldExtractorMapper.type, FDs, ReprKleisli],
    sequencer: RecordSequencer.Aux[ReprKleisli, Kleisli[F, S, Repr]],
    un: Unapply.Aux1[Functor, F[Repr], F, Repr]
  ): Extractor[F, S, T] = new Extractor[F, S, T] {

    def apply(): Kleisli[F, S, T] = {
      implicit val functor: Functor[F] = un.TC
      sequencer(mapper(ccd.fields)).map(ccd.gen.from)
    }
  }

  def apply[F[_], S, T](implicit ex: Extractor[F, S, T]): Extractor[F, S, T] = ex

}

trait ExtractorSyntax {

  class extractC[F[_], T]() {
    def apply[S](s: S)(implicit e: Extractor[F, S, T]): F[T] = e.extract(s)
  }

  def extract[F[_], T] = new extractC[F, T]
}
