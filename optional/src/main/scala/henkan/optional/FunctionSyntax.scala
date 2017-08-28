package henkan.optional

import cats.Functor
import cats.data.NonEmptyList
import syntax.all._

trait FunctionSyntax {
  final class autoOptionalPartial[OptionalA, OptionalB] private[optional] {
    def apply[F[_], A, B](f: A ⇒ F[B])(whenMissing: NonEmptyList[RequiredFieldMissing] ⇒ F[OptionalB])(
      implicit
      toOptional: ToOptional[B, OptionalB],
      validateFromOptional: ValidateFromOptional[OptionalA, A],
      F: Functor[F]
    ): OptionalA ⇒ F[OptionalB] = (oa: OptionalA) ⇒ {
      validate(oa).to[A].fold(
        whenMissing,
        a ⇒ F.map(f(a))(from(_).toOptional[OptionalB])
      )
    }
  }

  def autoOptional[OptionalA, OptionalB] = new autoOptionalPartial[OptionalA, OptionalB]

  implicit class henkanOptionalOps[A, F[_]: Functor, B](f: A ⇒ F[B]) {
    def toOptional[OptionalA, OptionalB](
      whenMissing: NonEmptyList[RequiredFieldMissing] ⇒ F[OptionalB]
    )(
      implicit
      toOptional: ToOptional[B, OptionalB],
      validateFromOptional: ValidateFromOptional[OptionalA, A]
    ) = autoOptional[OptionalA, OptionalB](f)(whenMissing)
  }
}
