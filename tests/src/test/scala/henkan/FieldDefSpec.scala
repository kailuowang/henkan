package henkan

import org.specs2.mutable.Specification

case class MyClassWithoutDfts(foo: String, bar: Int)

class FieldDefinitionSpec extends Specification {
  "Def without Defaults" >> {
    val ccd = CaseClassDefinition[MyClassWithoutDfts]
    1 === 1
  }
}
