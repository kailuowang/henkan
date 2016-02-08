package henkan

import algebra.{Semigroup, Monoid}
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

    def fieldReader[T] = FieldReader { (m: Map[String, Any], field: String) ⇒
      m.get(field).flatMap(safeCast[T])
    }
    implicit val fint = fieldReader[Int]
    implicit val fMap = fieldReader[Map[String, Any]]
    implicit val fString = fieldReader[String]

    val data = Map[String, Any]("foo1" → "parent", "child" → Map[String, Any]("foo" → "a", "bar" → 2))

    extract[Option, MyParent](data) must beSome(MyParent("parent", MyClass("a", 2)))

    val data2 = Map[String, Any]("foo1" → "parent", "child" → Map[String, Any]("foo" → "a"))

    extract[Option, MyParent](data2) must beNone

  }

}

class ExporterSpec extends Specification {
  import ExporterSyntax._
  def fieldWriter[T] = FieldWriter { (fieldName: FieldName, v: T) ⇒
    Map[String, Any](fieldName → v)
  }

  implicit val feInt = fieldWriter[Int]
  implicit val feString = fieldWriter[String]
  implicit val feRecursive = fieldWriter[Map[String, Any]]

  implicit val m = new Semigroup[Map[String, Any]] {
    def combine(x: Map[String, Any], y: Map[String, Any]): Map[String, Any] = x ++ y
  }

  "export single level class" >> {

    val result = export[MyClass, Map[String, Any]](MyClass("foo1", 34))
    result === Map[String, Any]("foo" → "foo1", "bar" → 34)
  }

  "export hiearchical data" >> {
    val result = export[MyParent, Map[String, Any]](MyParent("parentFoo", MyClass("childFoo", 34)))
    result === Map[String, Any]("foo1" → "parentFoo", "child" → Map[String, Any]("foo" → "childFoo", "bar" → 34))
  }
}
