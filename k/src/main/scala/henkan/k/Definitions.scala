package henkan.k

import cats.Monad
import cats.data.{Kleisli, XorT}
import cats.implicits._
import cats.sequence._
import shapeless._
import shapeless.ops.hlist._
import shapeless.record._
import scala.concurrent.Future

trait Definitions {
  type Reason //reason ADT for failure

  /**
   * Effect of the operations, e.g. [[Future]]
   */
  type Effect[_]

  implicit def effectMonad: Monad[Effect]

  type Result[T] = XorT[Effect, Reason, T]

  type K[A, B] = Kleisli[Result, A, B]

  type L[B] = K[Unit, B]

  object K extends KFunctions with KOps

  object Result {
    def left[T](r: Reason): Result[T] = XorT.left[Effect, Reason, T](effectMonad.pure(r))
    def pure[T] = XorT.pure[Effect, Reason, T] _
  }

  object KFunctions extends KFunctions
  trait KFunctions {

    implicit def of[A, B](f: A ⇒ Result[B]): K[A, B] = Kleisli(f)

    def hNil[A]: K[A, HNil] = apply(_ ⇒ HNil)

    def apply[A, B](f: A ⇒ B): K[A, B] = f map XorT.pure[Effect, Reason, B]

    def pure[A] = {
      class PureOps {
        def apply[B](b: B): K[A, B] = K((_: A) ⇒ b)
      }
      new PureOps
    }

    /**
     * K whose B type is a [[Record]] composed of several Ks
     * e.g.
     * {{{
     * val kl: K[String, Int] = K(_.length))
     * val composed = K.compose(length = kl)
     *
     * }}}
     */
    def compose = sequenceRecord

    def composeTo[T] = sequenceGeneric[T]

    def combine[A, L1 <: HList, L2 <: HList, B <: HList](self: K[A, L1], that: K[A, L2])(
      implicit
      prepend: Prepend.Aux[L1, L2, B]
    ): K[A, B] = {
      (self |@| that) map (_ ++ _)
    }
  }

  trait KOps {
    import KFunctions._

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

    implicit class lOpsGeneral[B](self: L[B]) {
      def run0 = self.run(())

      def contraMapTo[T] = self.contraMap[T](_ ⇒ ())
    }

  }
}
