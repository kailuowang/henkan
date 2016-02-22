package henkan.extractor

import cats._
import cats.data.Kleisli
import henkan._
import scala.annotation.unchecked.{uncheckedVariance ⇒ uV}

import scala.collection.GenTraversableOnce
import scala.collection.generic.CanBuildFrom

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

trait lowPriorityMk {
  implicit def mkFromMap[F[_], S, RT, T](
    implicit
    fr: FieldReader[F, S, RT],
    frm: FieldReaderMapper[RT, T],
    un: Unapply.Aux1[Functor, F[RT], F, RT]
  ): FieldReader[F, S, T] = fr.map(frm.apply)

  implicit def mkFromFlatMap[F[_], S, RT, T](
    implicit
    fr: FieldReader[F, S, RT],
    frm: FieldReaderMapper[RT, F[T]],
    un: Unapply.Aux1[FlatMap, F[RT], F, RT]
  ): FieldReader[F, S, T] = fr.flatMap(frm.apply)

}

object FieldReader extends lowPriorityMk {

  def apply[F[_], S, T](f: (S, FieldName) ⇒ F[T]) = new FieldReader[F, S, T] {
    def apply(fieldName: FieldName): Kleisli[F, S, T] =
      Kleisli(s ⇒ f(s, fieldName))
  }

  implicit def apply[F[_], S, T](f: FieldName ⇒ Kleisli[F, S, T]) = new FieldReader[F, S, T] {
    def apply(fieldName: FieldName): Kleisli[F, S, T] = f(fieldName)
  }

}

trait FieldReaderMapper[T, U] {
  def apply(t: T): U
}

object FieldReaderMapper {
  implicit def apply[T, U](f: T ⇒ U) = new FieldReaderMapper[T, U] {
    def apply(t: T): U = f(t)
  }

  implicit def mkFromExtractor[F[_], S, U](
    implicit
    extractor: Extractor[F, S, U]
  ): FieldReaderMapper[S, F[U]] = extractor.extract _
}
