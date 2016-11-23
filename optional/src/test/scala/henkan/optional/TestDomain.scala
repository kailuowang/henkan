package henkan.optional

import cats._

object TestDomain {
  implicit def autoEq[T <: Product]: Eq[T] = Eq.fromUniversalEquals

  case class Message(a: Option[String], b: Option[Int])

  case class ParentMessage(a: Option[Double], child: Option[Message])
  case class ParentDomain(a: Double, child: Domain)

  case class ListMessage(a: Option[Double], children: Option[List[Message]])
  case class ListDomain(a: Double, children: List[Domain])

  case class MessageMissingB(a: Option[String])
  case class Domain(a: String, b: Int)
  case class DomainWithOptionalB(a: String, b: Option[Int])
  case class DomainMissingB(a: String)

  case class MessageWithRequiredField(a: Option[String], b: List[Int])
  case class DomainWithAllFieldsRequired(a: String, b: List[Int])

}
