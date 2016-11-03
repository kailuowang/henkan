package henkan.optional

import cats.data.{ValidatedNel, NonEmptyList, Validated}
import org.specs2.mutable.Specification
import cats.implicits._
import TestDomain._
import shapeless.test.illTyped
import syntax.fromOptional._

class ValidateFromOptionalSpec extends Specification {

  def missingFields[T](fn: String*): ValidatedNel[RequiredFieldMissing, T] =
    Validated.invalid(NonEmptyList.fromList(fn.toList.map(RequiredFieldMissing)).get)

  "translate instance with all required fields present" >> {
    validate(Message(Some("dfd"), Some(3))).to[Domain] must_== Validated.Valid(Domain("dfd", 3))
  }

  "translate instance with some fields empty" >> {
    validate(Message(Some("dfd"), None)).to[Domain] must_== missingFields("b")
  }

  "translate instance with all fields empty" >> {
    validate(Message(None, None)).to[Domain] must_== missingFields("a", "b")
  }

  "accept empty fields when they are optional in the target" >> {
    validate(Message(Some("dfd"), None)).to[DomainWithOptionalB] must_== Validated.Valid(DomainWithOptionalB("dfd", None))
  }

  "does not compile with missing fields" >> {
    illTyped("implicitly[ConvertFromOptional[MessageMissingB, Domain]]")
    1 must_== 1
  }

  "translate instance with nested fields" >> {
    validate(ParentMessage(Some(1d), Some(Message(Some("a"), Some(1))))).to[ParentDomain] must_==
      Validated.Valid(ParentDomain(1d, Domain("a", 1)))
  }

  "translate instance with nested fields with missing field" >> {
    validate(ParentMessage(Some(1d), Some(Message(Some("a"), None)))).to[ParentDomain] must_==
      missingFields("b")
  }

  "translate instance with nested list fields" >> {
    validate(ListMessage(Some(1d), Some(List(Message(Some("a"), Some(1)))))).to[ListDomain] must_==
      Validated.Valid(ListDomain(1d, List(Domain("a", 1))))
  }

}

