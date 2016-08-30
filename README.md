[![Build Status](https://travis-ci.org/kailuowang/henkan.svg)](https://travis-ci.org/kailuowang/henkan)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/94b5ef789e73441ca101c5d0e083aef6)](https://www.codacy.com/app/kailuo-wang/henkan)
[![Codacy Badge](https://api.codacy.com/project/badge/coverage/94b5ef789e73441ca101c5d0e083aef6)](https://www.codacy.com/app/kailuo-wang/henkan)
[![Stories in Ready](https://badge.waffle.io/kailuowang/henkan.svg?label=ready&title=Ready)](http://waffle.io/kailuowang/henkan)
[ ![Download](https://api.bintray.com/packages/kailuowang/maven/henkan/images/download.svg) ](https://bintray.com/kailuowang/maven/henkan/_latestVersion)

# Henkan [変換]

A small proof-of-concept library that provides generic and yet typesafe transformation between case classes, case class and runtime data types (Such as Map, JsonObject, Typesafe.Config, etc) .

## Known issues

* [Error when the last field is a nested class](https://github.com/kailuowang/henkan/issues/15)

Behind the scene, henkan uses [shapeless](https://github.com/milessabin/shapeless) [cats](https://github.com/typelevel/cats) and [kittens](https://github.com/milessabin/kittens). No marcos was used directly.

Henkan can

1. transform between case classes, which minimize the need to manually using constructor to transform information from one case class to another.

  *Features*:

  a. quick transformation when the source case class has all the fields the target case class has: e.g. `a.to[B]()`

  b. supplement (if source case class doesn't have a field) or override field values. e.g. `a.to[B].set(foo = "bar")`

  c. use the default values of the target case classes if needed

2. transform between a runtime data type and a case class. Usually this type of transformation is done either manually or through some macro generated transformers. Using shapeless can achieve this as well, henkan is providing a generic transformer library on top of shapeless, which minimizes the boilerplate needed. However this part is also experimental and, as of now, limited than the macro solution.

  *Features*:

  a. transform any runtime data type to an arbitrary Monad of taget case class - you just need to provide some `FieldReader`s that can read primitive values out of the runtime data type given a field name.

  b. supports default value.

  c. support recursive case classes, i.e. case class that has case class fields.

## Get started 

```scala
 resolvers += Resolver.bintrayRepo("kailuowang", "maven")

 libraryDependencies += "com.kailuowang" %% "henkan" % "0.1.0"
```

## Examples

### Transform between case classes


```scala
import java.time.LocalDate

case class Employee(name: String, address: String, dateOfBirth: LocalDate, salary: Double = 50000d)

case class UnionMember(name: String, address: String, dateOfBirth: LocalDate)

val employee = Employee("George", "123 E 86 St", LocalDate.of(1963, 3, 12), 54000)

val unionMember = UnionMember("Micheal", "41 Dunwoody St", LocalDate.of(1994, 7, 29))
```

Now use the henkan magic to transform between `UnionMember` and `Employee`
```scala
scala> import henkan.convert.Syntax._
import henkan.convert.Syntax._

scala> employee.to[UnionMember]()
res4: UnionMember = UnionMember(George,123 E 86 St,1963-03-12)

scala> unionMember.to[Employee]()
res5: Employee = Employee(Micheal,41 Dunwoody St,1994-07-29,50000.0)

scala> unionMember.to[Employee].set(salary = 60000.0)
res6: Employee = Employee(Micheal,41 Dunwoody St,1994-07-29,60000.0)
```
Missing fields will fail the compilation
```scala
scala> case class People(name: String, address: String)
defined class People

scala> val people = People("John", "49 Wall St.")
people: People = People(John,49 Wall St.)
```
```scala
scala> people.to[Employee]() //missing DoB
<console>:22: error: 
    You have not provided enough arguments to convert from People to Employee.
    shapeless.HNil

       people.to[Employee]() //missing DoB
                          ^
```
Wrong argument types will fail the compilation
```scala
scala> unionMember.to[Employee].set(salary = 60) //salary was input as Int rather than Double
<console>:22: error: One or more fields in shapeless.::[shapeless.labelled.FieldType[shapeless.tag.@@[Symbol,String("salary")],Int],shapeless.HNil] is not in Employee
error after rewriting to henkan.convert.Syntax.convert[UnionMember](unionMember).to[Employee].set.applyDynamicNamed("apply")(scala.Tuple2("salary", 60))
possible cause: maybe a wrong Dynamic method signature?
       unionMember.to[Employee].set(salary = 60) //salary was input as Int rather than Double
                                   ^
```


### Transform between runtime data types and case class

Suppose you have some case classes
```scala
case class MyClass(foo: String, bar: Int)

case class MyParent(foo1: String, child: MyClass)
```
And you want to transform them from a Map[String, Any]
```scala
val data = Map[String, Any]("foo1" → "parent", "child" → Map[String, Any]("foo" → "a", "bar" → 2))
```

Then first lets write some primitive readers. Note that it 's you that dictate the source type `Map[String, Any]` and High kinded container type `Option`

```scala
import cats.implicits._
import scala.util.Try
import henkan.extractor._

def safeCast[T](t: Any): Option[T] = Try(t.asInstanceOf[T]).toOption

def myFieldReader[T] = FieldReader { (m: Map[String, Any], field: String) ⇒
  m.get(field).flatMap(safeCast[T])
}
implicit val fint = myFieldReader[Int]
implicit val fString = myFieldReader[String]
implicit val fMap = myFieldReader[Map[String, Any]] // need this to recursively extract case classes
```

Now you can extract any case classes with String or Int fields from the Map[String, Any] data

```scala
scala> extract[Option, MyParent](data)
res3: Option[MyParent] = Some(MyParent(parent,MyClass(a,2)))
```

### Other examples can be found in [examples](examples/src/main/scala/henkan/example) including a typesafe config transformer

