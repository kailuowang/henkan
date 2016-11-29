package henkan.optional

import org.specs2.mutable.Specification
import cats.implicits._
import TestDomain._
import syntax.toOptional._

class ToOptionalSpec extends Specification {

  "convert flat instance" >> {
    from(Domain("dfd", 23)).toOptional[Message] must_== Message(Some("dfd"), Some(23))
  }

  "convert domain with missing fields" >> {
    from(DomainMissingB("dfd")).toOptional[Message] must_== Message(Some("dfd"), None)
  }

  "convert domain with optional fields" >> {
    from(DomainWithOptionalB("dfd", Some(3))).toOptional[Message] must_== Message(Some("dfd"), Some(3))
    from(DomainWithOptionalB("dfd", None)).toOptional[Message] must_== Message(Some("dfd"), None)
  }

  "convert nested domain" >> {
    from(ParentDomain(11d, Domain("dfd", 23)))
      .toOptional[ParentMessage] must_== ParentMessage(
        Some(11d), Some(Message(Some("dfd"), Some(23)))
      )
  }

  "convert  domain with Functor fields" >> {
    from(ListDomain(11d, List(Domain("dfd", 23))))
      .toOptional[ListMessage] must_== ListMessage(
        Some(11d), Some(List(Message(Some("dfd"), Some(23))))
      )
  }

  "convert domain with Functor fields direct" >> {
    from(ListDomain(11d, List(Domain("dfd", 23))))
      .toOptional[ListMessageDirect] must_== ListMessageDirect(
        Some(11d), List(Message(Some("dfd"), Some(23)))
      )
  }

  "convert domain with Functor fields with complex item type" >> {
    from(ListMixedDomain(11d, List(DomainWithOptionalB("dfd", Some(23)))))
      .toOptional[ListMixedMessage] must_== ListMixedMessage(
        Some(11d), List(Message(Some("dfd"), Some(23)))
      )
  }

  "convert to Message with required fields" >> {
    from(DomainWithAllFieldsRequired("a", List(1))).toOptional[MessageWithRequiredField] must_==
      MessageWithRequiredField(Some("a"), List(1))
  }

  "convert to Message with mixed fields" >> {
    from(DomainWithMixedField("a", List(1), Some(1d))).toOptional[MessageWithMixedField] must_==
      MessageWithMixedField(Some("a"), List(1), Some(1d))
  }

  "convert to Message with deep nested field" >> {
    from(NestedGrand("a", NestedParent("b", NestedChild("c")))).toOptional[NestedGrandMsg] must_==
      NestedGrandMsg(Some("a"), Some(
        NestedParentMsg(Some("b"), Some(NestedChildMsg(Some("c"))))
      ))
  }

  "convert to Message with list of deep nested item" >> {
    from(ListNested("l", Some(1), List(NestedGrand("a", NestedParent("b", NestedChild("c")))), Some(Domain("a", 2)))).toOptional[ListNestedMsg] must_==
      ListNestedMsg(Some("l"), Some(1), List(NestedGrandMsg(Some("a"), Some(
        NestedParentMsg(Some("b"), Some(NestedChildMsg(Some("c"))))
      ))), Some(Message(Some("a"), Some(2))))
  }

}
