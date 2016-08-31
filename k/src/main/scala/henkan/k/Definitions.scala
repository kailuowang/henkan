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

  implicit def effectApplicative: Monad[Effect]

  type Result[T] = XorT[Effect, Reason, T]

  type K[A, B] = Kleisli[Result, A, B]

  object K extends KFunctions with KOps

  object KFunctions extends KFunctions
  trait KFunctions {

    implicit def fromFunction[A, B](f: A ⇒ Result[B]): K[A, B] = Kleisli(f)

    def hNil[A]: K[A, HNil] = apply(_ ⇒ HNil)

    def apply[A, B](f: A ⇒ B): K[A, B] = f map XorT.pure[Effect, Reason, B]

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
    implicit class extractorOpsHList[A, B <: HList](self: K[A, B]) {

      def and[ThatR <: HList, ResultR <: HList](that: K[A, ThatR])(
        implicit
        prepend: Prepend.Aux[B, ThatR, ResultR]
      ): K[A, ResultR] = combine(self, that)

      def to[T](implicit gen: LabelledGeneric.Aux[T, B]): K[A, T] = self.map(gen.from)
    }

  }
}
