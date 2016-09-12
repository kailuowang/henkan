package henkan.exporter

import algebra.Semigroup
import henkan.FieldName
import henkan.extractor.{MyParent, MyClass}
import org.specs2.mutable.Specification

class ExporterSpec extends Specification {

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

  "export hierarchical data" >> {
    val result = export[MyParent, Map[String, Any]](MyParent("parentFoo", MyClass("childFoo", 34)))
    result === Map[String, Any]("foo1" → "parentFoo", "child" → Map[String, Any]("foo" → "childFoo", "bar" → 34))
  }
}
