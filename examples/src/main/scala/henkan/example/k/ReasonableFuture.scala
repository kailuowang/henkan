package henkan.example.k

import cats.Monad

import scala.concurrent.Future

object ReasonableFuture extends henkan.k.Definitions {
  sealed trait MyReason

  case class ExceptionOccurred(msg: String, ex: Option[Exception]) extends MyReason
  case class UserError(msg: String) extends MyReason
  case class Unavailable(missingThingy: String) extends MyReason

  type Reason = MyReason

  type Effect[X] = Future[X]

  implicit def effectApplicative: Monad[Effect] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    cats.std.future.futureInstance
  }
}

object MyApp extends App {
  import ReasonableFuture._
  import K._
  case class Result(l: Int, v: String)

  val kl: K[String, Int] = K(_.length)

  val kv: K[String, String] = K(identity)

  val myK: K[String, Result] = compose(l = kl, v = kv).to[Result]

  myK.run(args.head).map(println)

}
