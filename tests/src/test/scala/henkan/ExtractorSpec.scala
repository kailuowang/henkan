package henkan

import cats.Applicative
import cats.data._
import org.specs2.mutable.Specification
import shapeless.LabelledGeneric
import shapeless.labelled._
import shapeless._
import ops.hlist._
import ops.record._
import cats.implicits._

case class MyClass(foo: String, bar: Int)

class ExtractorSpec extends Specification {

  type StringMap = Map[String, String]

  "extract Option from String map" >> {

    implicit val frI = FieldReader((m: StringMap, field: String) ⇒ m.get(field).map(_.toInt))

    implicit val frString = FieldReader((m: StringMap, field: String) ⇒ m.get(field))

    Extractor.extract[Option, MyClass](Map[String, String]("foo" → "a", "bar" → "2")) must beSome(MyClass("a", 2))

    Extractor.extract[Option, MyClass](Map[String, String]("foo" → "a")) must beNone

  }

}
