[![Build Status](https://travis-ci.org/kailuowang/henkan.svg)](https://travis-ci.org/kailuowang/henkan)
[![Coverage Status](https://coveralls.io/repos/github/kailuowang/henkan/badge.svg?branch=master)](https://coveralls.io/github/kailuowang/henkan?branch=master)
[![Latest version](https://index.scala-lang.org/kailuowang/henkan/henkan-convert/latest.svg?color=orange)](https://index.scala-lang.org/kailuowang/henkan/henkan-optional)
# Henkan [変換]

A small library for converting between case classes. 

## Modules

### `henkan.convert`

Transform between case classes, which minimize the need to manually using constructor to transform information from one case class to another.

  *Features*:

 1. quick transformation when the source case class has all the fields the target case class has: e.g. `a.to[B]()`

 2. supplement (if source case class doesn't have a field) or override field values. e.g. `a.to[B].set(foo = "bar")`

 3. use the default values of the target case classes if needed


### `henkan.optional`

Conversion between case classes with optional fields and case class with required fields. One of the use cases for such conversions is conversion between scalaPB generated classes where most fields are Options and internal case classes where you have required fields.


## Get started

```scala

 libraryDependencies += "com.kailuowang" %% "henkan-convert" % "0.4.0"

 libraryDependencies += "com.kailuowang" %% "henkan-optional" % "0.4.0"
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

## Contributors and participation

henkan is currently maintained by [Kailuo Wang][kailuowang].

Any form of contribution (issue report, PR, etc) is more than welcome.

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


[apache2]: http://www.apache.org/licenses/LICENSE-2.0
[kailuowang]: http://twitter.com/kailuowang
[typelevel]: http://typelevel.org/
[typelevel-coc]: http://typelevel.org/conduct.html
[kittens]: http://github.com/milessabin/kittens
[shapeless]: http://github.com/milessabin/shapeless
[cats]: http://github.com/typelevel/cats
