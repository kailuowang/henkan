package henkan.example.k

import cats.Monad
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object ReasonableFuture extends henkan.k.Definitions {
  sealed trait MyReason

  case class ExceptionOccurred(msg: Option[String], ex: Option[Throwable]) extends MyReason
  case class UserError(msg: String) extends MyReason
  case class Unavailable(missingThingy: String) extends MyReason

  type Reason = MyReason

  type Effect[X] = Future[X]

  implicit val effectMonad: Monad[Effect] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    cats.instances.future.catsStdInstancesForFuture
  }

  object ResultTransformations {
    implicit class optionToResult[A](self: Option[A]) {
      def toResult(ifEmpty: Reason) = self.fold[Result[A]](Result.left(ifEmpty))(Result.pure)
    }
    implicit class tryToResult[A](self: Try[A]) {
      def toResult(ifFail: PartialFunction[Throwable, Reason] = PartialFunction.empty): Result[A] =
        self match {
          case Success(a) ⇒ Result.pure(a)
          case Failure(e) ⇒ Result.left[A](ifFail.lift(e).getOrElse(ExceptionOccurred(None, Some(e))))
        }

    }
  }
}

