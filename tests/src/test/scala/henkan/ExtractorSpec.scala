package henkan

import org.specs2.mutable.Specification

import cats.implicits._

import scala.util.Try

case class MyClass(foo: String, bar: Int)

case class MyParent(foo1: String, child: MyClass)

class ExtractorSpec extends Specification {

  "extract Option from String map" >> {

    implicit val frInt = FieldReader((m: Map[String, String], field: String) ⇒ m.get(field).map(_.toInt))

    implicit val frString = FieldReader((m: Map[String, String], field: String) ⇒ m.get(field))

    extract[Option, MyClass](Map("foo" → "a", "bar" → "2")) must beSome(MyClass("a", 2))

    extract[Option, MyClass](Map("foo" → "a")) must beNone

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

    extract[Option, MyParent](data) must beSome(MyParent("parent", MyClass("a", 2)))

    val data2 = Map[String, Any]("foo1" → "parent", "child" → Map[String, Any]("foo" → "a"))

    extract[Option, MyParent](data2) must beNone

  }

}

