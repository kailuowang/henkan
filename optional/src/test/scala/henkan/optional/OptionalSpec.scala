package henkan.optional

import cats.{Traverse, Eq}
import cats.data.{ValidatedNel, NonEmptyList, Validated}
import henkan.optional.OptionalSpec._
import org.specs2.mutable.Specification
import cats.implicits._
import OptionalSpec._
import shapeless.Witness
import shapeless._
import shapeless.labelled._
import shapeless.ops.record.Selector
import shapeless.record.Record
import shapeless.test.illTyped
import ConvertFromOptional.validate

class OptionalSpec extends Specification {

  def missingFields[T](fn: String*): ValidatedNel[RequiredFieldMissing, T] =
    Validated.invalid(NonEmptyList.fromList(fn.toList.map(RequiredFieldMissing)).get)

  "translate instance with all required fields present" >> {
    validate(Message(Some("dfd"), Some(3))).to[Domain] === Validated.Valid(Domain("dfd", 3))
  }

  "translate instance with some fields empty" >> {
    validate(Message(Some("dfd"), None)).to[Domain] === missingFields("b")
  }

  "translate instance with all fields empty" >> {
    validate(Message(None, None)).to[Domain] === missingFields("a", "b")
  }

  "accept empty fields when they are optional in the target" >> {
    validate(Message(Some("dfd"), None)).to[DomainWithOptionalB] === Validated.Valid(DomainWithOptionalB("dfd", None))
  }

  "does not compile with missing fields" >> {
    illTyped("implicitly[ConvertFromOptional[MessageMissingB, Domain]]")
    1 === 1
  }

  "translate instance with nested fields" >> {
    validate(ParentMessage(Some(1d), Some(Message(Some("a"), Some(1))))).to[ParentDomain] ===
      Validated.Valid(ParentDomain(1d, Domain("a", 1)))
  }

  "translate instance with nested fields with missing field" >> {
    validate(ParentMessage(Some(1d), Some(Message(Some("a"), None)))).to[ParentDomain] ===
      missingFields("b")
  }

  "translate instance with nested list fields" >> {
    validate(ListMessage(Some(1d), Some(List(Message(Some("a"), Some(1)))))).to[ListDomain] ===
      Validated.Valid(ListDomain(1d, List(Domain("a", 1))))
  }

}

object OptionalSpec {
  implicit def autoEq[T <: Product]: Eq[T] = Eq.fromUniversalEquals

  //  type R = Record.`'a -> Option[String], 'b -> Option[Int]`.T  todo: remove after debug finish
  case class Message(a: Option[String], b: Option[Int])

  case class ParentMessage(a: Option[Double], child: Option[Message])
  case class ParentDomain(a: Double, child: Domain)

  case class ListMessage(a: Option[Double], children: Option[List[Message]])
  case class ListDomain(a: Double, children: List[Domain])

  case class MessageMissingB(a: Option[String])
  case class Domain(a: String, b: Int)
  case class DomainWithOptionalB(a: String, b: Option[Int])
}
