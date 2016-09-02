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
  case class Report(l: Int, v: String)

  val kl: K[String, Int] = K(_.length)

  val kv: K[String, String] = K(identity)

  val kIsOdd: K[Int, Boolean] = K(_ % 2 == 1)

  val kOddLength: K[String, Boolean] = kl andThen kIsOdd

  val reportK: K[String, Report] = compose(l = kl, v = kv).to[Report]

  val getArgsHead: K[Array[String], String] = (a: Array[String]) ⇒
    a.headOption.fold(Result.left[String](UserError("No args")))(Result.pure)

  val myK = getArgsHead andThen reportK

  myK.run(args).fold(
    println,
    println
  )

}
