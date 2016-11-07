package henkan.optional

import cats.{Applicative, ApplicativeError}
import org.specs2.mutable.Specification
import cats.implicits._
import TestDomain._
import henkan.optional.syntax.function._

class FunctionSyntaxSpec extends Specification {
  override def is = s2"""
    FunctionSyntax
     wraps function with Optional Types       $wrapFunction
  """

  def wrapFunction = {
    val f: Domain ⇒ Either[String, ParentDomain] = (d: Domain) ⇒ Right(ParentDomain(1d, d))

    val fm = f.toOptional[Message, ParentMessage](es ⇒ Left("Missing fields: " + es.map(_.fieldName).toList.mkString(", ")))

    fm(Message(Some("a"), Some(1))) must_== Right(ParentMessage(Some(1d), Some(Message(Some("a"), Some(1)))))

    fm(Message(Some("a"), None)) must_== Left("Missing fields: b")
  }
}

