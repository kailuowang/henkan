```scala
import util.Try
// import util.Try

import shapeless._
// import shapeless._

import shapeless.ops.hlist.Mapper
// import shapeless.ops.hlist.Mapper

import cats.~>
// import cats.$tilde$greater

case class Algebra[F[_]](
  parseInt: String ⇒ F[Int],
  parseFloat: String ⇒ F[Float]
)
// warning: there was one feature warning; for details, enable `:setting -feature' or `:replay -feature'
// defined class Algebra

object Algebra1 {
  type F[T] = Try[T]

  val intepreter = Algebra[F](
    parseInt = (s: String) ⇒ Try(s.toInt),
    parseFloat = (s: String) ⇒ Try(s.toFloat)
  )
}
// defined object Algebra1

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
// warning: there were 6 feature warnings; for details, enable `:setting -feature' or `:replay -feature'
// defined object FunctorK

implicit val f: Try ~> Option = λ[Try ~> Option](_.toOption)
// f: scala.util.Try ~> Option = $anon$1@e80846a

val b: Algebra[Option] = FunctorK.mapK(Algebra1.intepreter)
// b: Algebra[Option] = Algebra(FunctorK$PolyMapK$$$Lambda$5117/560757496@581f743a,FunctorK$PolyMapK$$$Lambda$5117/560757496@4336ce76)

println(b.parseInt("3"))
// Some(3)

println(b.parseFloat("btr"))
// None
```
