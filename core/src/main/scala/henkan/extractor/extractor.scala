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

trait FieldReader[F[_], S, T] extends ((FieldName) ⇒ Kleisli[F, S, T]) {
  def map[U](f: T ⇒ U)(implicit unapply: Unapply.Aux1[Functor, F[T], F, T]): FieldReader[F, S, U] = {
    implicit val functor: Functor[F] = unapply.TC
    FieldReader(andThen(_.map(f)))
  }

  def flatMap[U](f: T ⇒ F[U])(implicit unapply: Unapply.Aux1[FlatMap, F[T], F, T]): FieldReader[F, S, U] = {
    implicit val fm: FlatMap[F] = unapply.TC
    FieldReader(andThen(_.flatMapF(f)))
  }

  /**
   * When T is a high kinded type RG[RT], this allows to map that G[U]
   */
  def mapK[G[_], RG[_], RT, U](c: RT ⇒ U)(
    implicit
    fg: Unapply.Aux1[Functor, T, RG, RT],
    evi: RG[U] <:< GenTraversableOnce[U],
    ff: Unapply.Aux1[Functor, F[T], F, T],
    cb: CanBuildFrom[Nothing, U, G[U @uV]]
  ): FieldReader[F, S, G[U]] = {
    map(g ⇒ evi(fg.TC.map(fg.subst(g))(c)).to[G])
  }
  /**
   * When T is a high kinded type RG[RT], this allows to map that G[U]
   */
  def flatMapK[G[_], RG[_], RT, U](c: RT ⇒ F[U])(
    implicit
    fg: Unapply.Aux1[Traverse, G[F[U]], G, F[U]],
    evi: T <:< GenTraversableOnce[RT],
    ff: Unapply.Aux1[Functor, G[RT], G, RT],
    ffm: Unapply.Aux1[FlatMap, F[T], F, T],
    ua: Unapply.Aux1[Applicative, F[T], F, T],
    cb: CanBuildFrom[Nothing, RT, G[RT]]
  ): FieldReader[F, S, G[U]] = {
    implicit val ap: Applicative[F] = ua.TC

    flatMap { t ⇒
      val gfu: G[F[U]] = ff.TC.map(evi(t).to[G])(c)
      fg.TC.sequence(gfu)
    }
  }

}

object FieldReader {

  def apply[F[_], S, T](f: (S, FieldName) ⇒ F[T]) = new FieldReader[F, S, T] {
    def apply(fieldName: FieldName): Kleisli[F, S, T] =
      Kleisli(s ⇒ f(s, fieldName))
  }

  implicit def apply[F[_], S, T](f: FieldName ⇒ Kleisli[F, S, T]) = new FieldReader[F, S, T] {
    def apply(fieldName: FieldName): Kleisli[F, S, T] = f(fieldName)

  }

  /**
   *  FieldReader.mapK has a better API
   *
   */
  def highKindedReader[RG[_], G[_], RT, T, F[_], S](c: RT ⇒ T)(
    implicit
    evi: RG[T] <:< GenTraversableOnce[T],
    fr: FieldReader[F, S, RG[RT]],
    fg: Unapply.Aux1[Functor, RG[RT], RG, RT],
    ff: Unapply.Aux1[Functor, F[RG[RT]], F, RG[RT]],
    cb: CanBuildFrom[Nothing, T, G[T]]
  ): FieldReader[F, S, G[T]] = fr.mapK(c)

}

trait ExtractorSyntax {

  class extractC[F[_], T]() {
    def apply[S](s: S)(implicit e: Extractor[F, S, T]): F[T] = e.extract(s)
  }

  def extract[F[_], T] = new extractC[F, T]
}
