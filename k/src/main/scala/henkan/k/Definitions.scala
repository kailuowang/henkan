package henkan.k

import cats.Monad
import cats.data.{Kleisli, EitherT}
import cats.implicits._
import cats.sequence._
import shapeless._
import shapeless.ops.hlist._
import shapeless.record._
import scala.concurrent.Future

trait Definitions {

  type Result[_]
  implicit def resultMonad: Monad[Result]

  type K[A, B] = Kleisli[Result, A, B]

  object K extends KBasicFunctions with KHListFunctions with KOps with KHListOps

  trait KBasicFunctions {

    implicit def of[A, B](f: A ⇒ Result[B]): K[A, B] = Kleisli(f)

    def apply[A, B](f: A ⇒ B): K[A, B] = f map resultMonad.pure

    def pure[A] = {
      class PureOps {
        def apply[B](b: B): K[A, B] = K((_: A) ⇒ b)
      }
      new PureOps
    }

    /**
     * returns an that can compose to K whose B type is a T with several Ks
     * e.g.
     * {{{
     * case class Foo(a: A, b: B)
     * val kA: K[Int, A]
     * val kB: K[Int, B]
     * val c = K.composeTo[Foo]
     * val composed : K[Int, Foo] = c(a = kA, b = kB)
     * }}}
     */
    def composeTo[T] = sequenceGeneric[T]
  }

  object KBasicFunctions extends KBasicFunctions

  trait KHListFunctions {
    self: KBasicFunctions ⇒
    def hNil[A]: K[A, HNil] = apply(_ ⇒ HNil)

    def combine[A, L1 <: HList, L2 <: HList, B <: HList](self: K[A, L1], that: K[A, L2])(
      implicit
      prepend: Prepend.Aux[L1, L2, B]
    ): K[A, B] = {
      (self |@| that) map (_ ++ _)
    }

    /**
     * returns K whose B type is a [[Record]] composed of several Ks
     * e.g.
     * {{{
     * val kl: K[String, Int] = K(_.length))
     * val composed = K.compose(length = kl)
     *
     * }}}
     */
    def compose = sequenceRecord

  }

  object KHListFunctions extends KHListFunctions with KBasicFunctions

  trait KOps {
    import KBasicFunctions._

    implicit class kOpsGeneral[A, B](self: K[A, B]) {
      def contraMap[C](f: C ⇒ A): K[C, B] = self.dimap(f)(identity)

      class contraMapRCls extends RecordArgs {
        def applyRecord[C, Args <: HList, Repr <: HList](args: Args)(
          implicit
          seq: RecordSequencer.Aux[Args, K[C, Repr]],
          lg: LabelledGeneric.Aux[A, Repr]
        ): K[C, B] = seq(args) andThen K((r: Repr) ⇒ lg.from(r)) andThen self
      }

      def contraMapR = new contraMapRCls

    }
  }

  trait KHListOps {
    import KHListFunctions._

    /**
     * Operations for K[A, B] where B is a HList
     */
    implicit class kOpsHList[A, B <: HList](self: K[A, B]) {

      def and[ThatR <: HList, ResultR <: HList](that: K[A, ThatR])(
        implicit
        prepend: Prepend.Aux[B, ThatR, ResultR]
      ): K[A, ResultR] = combine(self, that)

      def to[T](implicit gen: LabelledGeneric.Aux[T, B]): K[A, T] = self.map(gen.from)
    }

  }
}

trait DefinitionWithEitherT extends Definitions {

  type Reason //reason ADT for failure

  /**
   * Effect of the operations, e.g. [[Future]]
   */
  type Effect[_]

  type Result[T] = EitherT[Effect, Reason, T]

  implicit def effectMonad: Monad[Effect]

  implicit lazy val resultMonad: Monad[Result] = EitherT.catsDataMonadErrorForEitherT[Effect, Reason]

  object Result {
    def left[T](r: Reason): Result[T] = EitherT.left[Effect, Reason, T](effectMonad.pure(r))
    def pure[T] = EitherT.pure[Effect, Reason, T] _
  }

}
