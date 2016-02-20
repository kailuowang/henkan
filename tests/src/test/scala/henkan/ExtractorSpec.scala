package henkan.extractor

import algebra.{Semigroup, Monoid}
import alleycats.{Pure, EmptyK}
import cats.{Functor, Unapply}
import org.specs2.mutable.Specification
import henkan.syntax.all._

import cats.implicits._

import scala.collection.generic.CanBuildFrom
import scala.util.Try

case class MyClass(foo: String, bar: Int)

case class MyParent(foo1: String, child: MyClass)

case class CCWithDefault(foo: String = "a default value", bar2: Int)
case class CCWithDefaultParent(child: CCWithDefault, bar: Int = 42)

case class CCWithHighKindedType(foo: Vector[Int])

class ExtractorSpec extends Specification {

  "extract Option from String map" >> {

    implicit val frString = FieldReader((m: Map[String, String], field: String) ⇒ m.get(field))

    implicit val frInt = FieldReaderMapper((_: String).toInt)

    extract[Option, MyClass](Map("foo" → "a", "bar" → "2")) must beSome(MyClass("a", 2))

    extract[Option, MyClass](Map("foo" → "a")) must beNone

  }

  "extract from hierarchical data" >> {

    def safeCast[T](t: Any): Option[T] = Try(t.asInstanceOf[T]).toOption

    implicit val frAny = FieldReader { (_: Map[String, Any]).get(_: String) }
    def fieldMap[T] = FieldReaderMapper { safeCast[T](_: Any) }

    implicit val fint = fieldMap[Int]
    implicit val fMap = fieldMap[Map[String, Any]]
    implicit val fString = fieldMap[String]

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

    "extract high kinded type using FieldReader.mapK" >> {
      implicit val frString = FieldReader((m: Map[String, String], field: String) ⇒ m.get(field))

      implicit val frStringL = frString.map(_.split(",").toList)

      implicit val frIntL: FieldReader[Option, Map[String, String], Vector[Int]] = frStringL.mapK((_: String).toInt)

      val data = Map[String, String]("foo" → "1,2")
      extract[Option, CCWithHighKindedType](data) must beSome(CCWithHighKindedType(Vector(1, 2)))
    }

  }

}

case class MyParentK(foo1: String, children: Vector[MyClass])

class ExtractorKSpec extends Specification {

  "extract from hierarchical data" >> {

    def safeCast[T](t: Any): Option[T] = Try(t.asInstanceOf[T]).toOption

    def fieldReader[T] = FieldReader { (m: Map[String, Any], field: String) ⇒
      m.get(field).flatMap(safeCast[T])
    }
    implicit val fint = fieldReader[Int]
    implicit val fMapL = fieldReader[List[Map[String, Any]]]
    implicit val fString = fieldReader[String]

    "list of sub classes " >> {
      val data = Map[String, Any]("foo1" → "parent", "children" → List(Map[String, Any]("foo" → "a", "bar" → 2), Map[String, Any]("foo" → "b", "bar" → 3)))

      extract[Option, MyParentK](data) must beSome(MyParentK("parent", Vector(MyClass("a", 2), MyClass("b", 3))))

    }
  }

}
