```tut:book

import util.Try
import shapeless._
import shapeless.ops.hlist.Mapper
import cats.~>

case class Algebra[F[_]](
  parseInt: String ⇒ F[Int],
  parseFloat: String ⇒ F[Float]
)

object Algebra1 {
  type F[T] = Try[T]

  val intepreter = Algebra[F](
    parseInt = (s: String) ⇒ Try(s.toInt),
    parseFloat = (s: String) ⇒ Try(s.toFloat)
  )
}

object FunctorK {

  object PolyMapK extends Poly1 {
    implicit def caseF[A, F[_], G[_], B](implicit n: F ~> G) = at[A ⇒ F[B]] { f ⇒ (a: A) ⇒
      n(f(a))
    }
  }

  def mapK[A[_[_]], F[_], G[_], LAF <: HList, LAG <: HList](af: A[F])(
    implicit
    genF: Generic.Aux[A[F], LAF],
    genG: Generic.Aux[A[G], LAG],
    mapper: Mapper.Aux[PolyMapK.type, LAF, LAG]
  ): A[G] = {
    genG.from(mapper(genF.to(af)))
  }
}

implicit val f: Try ~> Option = λ[Try ~> Option](_.toOption)
val b: Algebra[Option] = FunctorK.mapK(Algebra1.intepreter)
println(b.parseInt("3"))
println(b.parseFloat("btr"))


```
