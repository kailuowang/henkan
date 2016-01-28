package henkan

import cats.Applicative
import cats.data._
import org.specs2.mutable.Specification
import shapeless.LabelledGeneric
import shapeless.labelled._
import shapeless._, shapeless.syntax.typeable._
import ops.hlist._
import ops.record._
import cats.implicits._

import scala.util.Try

case class MyClass(foo: String, bar: Int)

case class MyParent(foo1: String, child: MyClass)

class ExtractorSpec extends Specification {

  type StringMap = Map[String, String]

  "extract Option from String map" >> {

    implicit val frI = FieldReader((m: StringMap, field: String) ⇒ m.get(field).map(_.toInt))

    implicit val frString = FieldReader((m: StringMap, field: String) ⇒ m.get(field))

    Extractor.extract[Option, MyClass](Map[String, String]("foo" → "a", "bar" → "2")) must beSome(MyClass("a", 2))

    Extractor.extract[Option, MyClass](Map[String, String]("foo" → "a")) must beNone

  }

  "extract from hierarchical data" >> {
    def safeCast[T](t: Any): Option[T] = Try(t.asInstanceOf[T]).toOption

    def myFieldReader[T] = FieldReader { (m: Map[String, Any], field: String) ⇒
      m.get(field).flatMap(safeCast[T])
    }
    implicit val fint = myFieldReader[Int]
    implicit val fString = myFieldReader[String]
    implicit val decom = Decomposer((m: Map[String, Any], field: String) ⇒ m.get(field).flatMap(safeCast[Map[String, Any]]))

    val data = Map[String, Any]("foo1" → "parent", "child" → Map[String, Any]("foo" → "a", "bar" → 2))

    implicit val fe = Extractor[Option, Map[String, Any], MyClass]

    Extractor.extract[Option, MyParent](data) must beSome(MyParent("parent", MyClass("a", 2)))

    val data2 = Map[String, Any]("foo1" → "parent", "child" → Map[String, Any]("foo" → "a"))

    Extractor.extract[Option, MyParent](data2) must beNone

  }

}

