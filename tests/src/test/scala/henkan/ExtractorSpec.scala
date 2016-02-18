package henkan.extractor

import algebra.{Semigroup, Monoid}
import alleycats.{Pure, EmptyK}
import org.specs2.mutable.Specification
import henkan.all._

import cats.implicits._

import scala.util.Try

case class MyClass(foo: String, bar: Int)

case class MyParent(foo1: String, child: MyClass)

case class CCWithDefault(foo: String = "a default value", bar2: Int)
case class CCWithDefaultParent(child: CCWithDefault, bar: Int = 42)

class ExtractorSpec extends Specification {

  "extract Option from String map" >> {

    implicit val frInt = FieldReader((m: Map[String, String], field: String) ⇒ m.get(field).map(_.toInt))

    implicit val frString = FieldReader((m: Map[String, String], field: String) ⇒ m.get(field))

    extract[Option, MyClass](Map("foo" → "a", "bar" → "2")) must beSome(MyClass("a", 2))

    extract[Option, MyClass](Map("foo" → "a")) must beNone

  }

  "extract from hierarchical data" >> {

    def safeCast[T](t: Any): Option[T] = Try(t.asInstanceOf[T]).toOption

    def fieldReader[T] = FieldReader { (m: Map[String, Any], field: String) ⇒
      m.get(field).flatMap(safeCast[T])
    }
    implicit val fint = fieldReader[Int]
    implicit val fMap = fieldReader[Map[String, Any]]
    implicit val fString = fieldReader[String]

    "without default value" >> {

      val data = Map[String, Any]("foo1" → "parent", "child" → Map[String, Any]("foo" → "a", "bar" → 2))

      extract[Option, MyParent](data) must beSome(MyParent("parent", MyClass("a", 2)))

      val data2 = Map[String, Any]("foo1" → "parent", "child" → Map[String, Any]("foo" → "a"))

      extract[Option, MyParent](data2) must beNone
    }

    "with default value" >> {
      import alleycats.std.OptionInstances._

      val data = Map[String, Any]("child" → Map[String, Any]("bar2" → 2))

      extract[Option, CCWithDefaultParent](data) must beSome(CCWithDefaultParent(child = CCWithDefault(bar2 = 2)))
    }

    "with default value but no emptyK instance" >> {

      val data = Map[String, Any]("bar" → 32, "child" → Map[String, Any]("foo" → "b", "bar2" → 2))

      extract[Option, CCWithDefaultParent](data) must beSome(CCWithDefaultParent(bar = 32, child = CCWithDefault(foo = "b", bar2 = 2)))
    }
  }

}

