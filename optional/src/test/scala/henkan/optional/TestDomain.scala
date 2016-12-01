package henkan.optional

import cats._

object TestDomain {
  implicit def autoEq[T <: Product]: Eq[T] = Eq.fromUniversalEquals

  case class Message(a: Option[String], b: Option[Int])

  case class ParentMessage(a: Option[Double], child: Option[Message])
  case class ParentDomain(a: Double, child: Domain)

  case class ListMessage(a: Option[Double], children: Option[Seq[Message]])
  case class ListMessageDirect(a: Option[Double], children: Seq[Message])
  case class ListDomain(a: Double, children: Seq[Domain])

  case class ListMixedMessage(a: Option[Double], children: List[Message])
  case class ListMixedDomain(a: Double, children: List[DomainWithOptionalB])

  case class MessageMissingB(a: Option[String])
  case class Domain(a: String, b: Int)
  case class DomainWithOptionalB(a: String, b: Option[Int])

  case class MessageWithRequiredA(a: String, b: Option[Int])
  case class DomainMissingB(a: String)

  case class MessageWithRequiredField(a: Option[String], b: List[Int])
  case class DomainWithAllFieldsRequired(a: String, b: List[Int])

  case class MessageWithMixedField(a: Option[String], b: List[Int], c: Option[Double])
  case class DomainWithMixedField(a: String, b: List[Int], c: Option[Double])

  case class NestedGrand(name: String, child: NestedParent)
  case class NestedParent(name: String, child: NestedChild)
  case class NestedChild(name: String)

  case class NestedGrandMsg(name: Option[String], child: Option[NestedParentMsg])
  case class NestedParentMsg(name: Option[String], child: Option[NestedChildMsg])
  case class NestedChildMsg(name: Option[String])

  case class ListNested(name: String, age: Option[Int], children: List[NestedGrand], child: Option[Domain])
  case class ListNestedMsg(name: Option[String], age: Option[Int], children: List[NestedGrandMsg], child: Option[Message])

}
