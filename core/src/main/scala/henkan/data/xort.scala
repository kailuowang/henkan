package henkan.data

import FutureXor._
import cats.Monad
import cats.data.XorT

import scala.concurrent.ExecutionContext.Implicits._
import cats.std.future._
import scala.concurrent.Future

case class FutureXor[T](value: Wrapped[T])

object FutureXor {
  type Wrapped[T] = XorT[Future, String, T]
  implicit def toXorTO[T](ox: FutureXor[T]): Wrapped[T] = ox.value
  implicit def toFutureXOr[T](xo: Wrapped[T]): FutureXor[T] = FutureXor(xo)
  implicit val monad: Monad[FutureXor] = new Monad[FutureXor] {
    val xm = Monad[Wrapped]
    def pure[A](x: A): FutureXor[A] = xm.pure(x)

    def flatMap[A, B](fa: FutureXor[A])(f: (A) ⇒ FutureXor[B]): FutureXor[B] = xm.flatMap(fa)(f(_))
  }
}
