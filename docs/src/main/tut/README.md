[![Build Status](https://travis-ci.org/kailuowang/henkan.svg)](https://travis-ci.org/kailuowang/henkan)
[ ![Download](https://api.bintray.com/packages/kailuowang/maven/henkan-convert/images/download.svg) ](https://bintray.com/kailuowang/maven/henkan-convert/_latestVersion)
[![Coverage Status](https://coveralls.io/repos/github/kailuowang/henkan/badge.svg?branch=master)](https://coveralls.io/github/kailuowang/henkan?branch=master)

# Henkan [変換]

A small library to experiment generic functional programming with [kittens][kittens], [shapeless][shapeless] and [cats][cats].


## Contributors and participation

henkan is currently maintained by [Kailuo Wang][kailuowang].

The henkan project supports the [Typelevel][typelevel] [code of conduct][typelevel-coc]
and wants all of its channels (Gitter, GitHub, etc.) to be welcoming environments for
everyone.

## License

henkan is licensed under the [Apache License, Version 2.0][apache2]
(the "License"); you may not use this software except in compliance with
the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


## Modules

### `henkan.convert`

Transform between case classes, which minimize the need to manually using constructor to transform information from one case class to another.

  *Features*:

 1. quick transformation when the source case class has all the fields the target case class has: e.g. `a.to[B]()`

 2. supplement (if source case class doesn't have a field) or override field values. e.g. `a.to[B].set(foo = "bar")`

 3. use the default values of the target case classes if needed

### `henkan.extract`

Transform between a runtime data type and a case class. Usually this type of transformation is done either manually or through some macro generated transformers. Using shapeless can achieve this as well, henkan is providing a generic transformer library on top of shapeless, which minimizes the boilerplate needed. However this part is also experimental and, as of now, limited than the macro solution.

  *Features*:

1. transform any runtime data type to an arbitrary Monad of taget case class - you just need to provide some `FieldReader`s that can read primitive values out of the runtime data type given a field name.

2. supports default value.

3. support recursive case classes, i.e. case class that has case class fields.

  *Known issues for this feature*

  * [Error when the last field is a nested class](https://github.com/kailuowang/henkan/issues/15)


### `henkan.k`

Building blocks for generic function compositions.

### `henkan.optional`

Conversion between case classes with optional fields and case class with required fields. One of the use cases for such conversions is conversion between scalaPB generated classes where most fields are Options and internal case classes where you have required fields.



## Get started 

```scala

 libraryDependencies += "com.kailuowang" %% "henkan-extract" % "0.2.10"

 libraryDependencies += "com.kailuowang" %% "henkan-k" % "0.2.10"

 libraryDependencies += "com.kailuowang" %% "henkan-covert" % "0.2.10"

 libraryDependencies += "com.kailuowang" %% "henkan-optional" % "0.2.10"
```

## Examples

### Transform between case classes


```tut:silent
import java.time.LocalDate

case class Employee(name: String, address: String, dateOfBirth: LocalDate, salary: Double = 50000d)

case class UnionMember(name: String, address: String, dateOfBirth: LocalDate)

val employee = Employee("George", "123 E 86 St", LocalDate.of(1963, 3, 12), 54000)

val unionMember = UnionMember("Micheal", "41 Dunwoody St", LocalDate.of(1994, 7, 29))
```

Now use the henkan magic to transform between `UnionMember` and `Employee`
```tut:book
import henkan.convert.Syntax._

employee.to[UnionMember]()

unionMember.to[Employee]()

unionMember.to[Employee].set(salary = 60000.0)

```
Missing fields will fail the compilation
```tut:book
case class People(name: String, address: String)

val people = People("John", "49 Wall St.")
```
```tut:fail
people.to[Employee]() //missing DoB

```
Wrong argument types will fail the compilation
```tut:fail
unionMember.to[Employee].set(salary = 60) //salary was input as Int rather than Double

```


### Transform between runtime data types and case class

Suppose you have some case classes
```tut:silent:reset
case class MyClass(foo: String, bar: Int)

case class MyParent(foo1: String, child: MyClass)
```
And you want to transform them from a Map[String, Any]
```tut:silent
val data = Map[String, Any]("foo1" → "parent", "child" → Map[String, Any]("foo" → "a", "bar" → 2))
```

Then first lets write some primitive readers. Note that it 's you that dictate the source type `Map[String, Any]` and High kinded container type `Option`

```tut:silent
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

```tut:book
extract[Option, MyParent](data)
```

### Transform between case classes with optional field

`cats.optional` provides some facility to transform between case classes with optional fields and ones with required fields.
Suppose you have two case classes: `Message` whose fields are optional and `Domain` whose fields are required

```tut:silent:reset
case class Message(a: Option[String], b: Option[Int])
case class Domain(a: String, b: Int)
```
You can validate an instance of `Message` to a Validated `Domain`

```tut:silent
import cats.data.Validated
import cats.implicits._
import henkan.optional.all._
```

```tut:book
validate(Message(Some("a"), Some(2))).to[Domain]

validate(Message(Some("a"), None)).to[Domain]
```

The compilation will fail if the from case class doesn't have all fields the target case class needs
```tut:silent

case class MessageWithMissingField(a: Option[String])
```

```tut:fail
validate(MessageWithMissingField(Some("a"))).to[Domain]
```

You can convert in the opposite direction as well
```tut:book
from(Domain("a", 2)).toOptional[Message]
```

Note that if you from case class does not have all the fields the target class has, they will be set as `None`

```tut:silent
case class DomainWithMissingField(a: String)
```
```tut
from(DomainWithMissingField("a")).toOptional[Message]
```

`henkan.optional` supports nested case classes as well.

Note that if you are converting scalaPB generated case class, it generates `Seq` for repeated items, although the underlying implementation is actually List. `henkan.optional.all` has a `Traverse` instance for `Seq` but only works fine when the underlying implementation is either a `List` or `Vector`

### Other examples can be found in [examples](examples/src/main/scala/henkan/) including a typesafe config transformer

[apache2]: http://www.apache.org/licenses/LICENSE-2.0
[kailuowang]: http://twitter.com/kailuowang
[typelevel]: http://typelevel.org/
[typelevel-coc]: http://typelevel.org/conduct.html
[kittens]: http://github.com/milessabin/kittens
[shapeless]: http://github.com/milessabin/shapeless
[cats]: http://github.com/typelevel/cats
