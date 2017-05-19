[![Build Status](https://travis-ci.org/kailuowang/henkan.svg)](https://travis-ci.org/kailuowang/henkan)
[![Coverage Status](https://coveralls.io/repos/github/kailuowang/henkan/badge.svg?branch=master)](https://coveralls.io/github/kailuowang/henkan?branch=master)
[![Latest version](https://index.scala-lang.org/kailuowang/henkan/henkan-convert/latest.svg?color=orange)](https://index.scala-lang.org/kailuowang/henkan/henkan-optional)
# Henkan [変換]

A small library for converting between case classes. 


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


### `henkan.optional`

Conversion between case classes with optional fields and case class with required fields. One of the use cases for such conversions is conversion between scalaPB generated classes where most fields are Options and internal case classes where you have required fields.



## Get started 

```scala

 libraryDependencies += "com.kailuowang" %% "henkan-convert" % "0.2.10"

 libraryDependencies += "com.kailuowang" %% "henkan-optional" % "0.2.10"
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
import henkan.convert.Syntax._
// import henkan.convert.Syntax._

employee.to[UnionMember]()
// res4: UnionMember = UnionMember(George,123 E 86 St,1963-03-12)

unionMember.to[Employee]()
// res5: Employee = Employee(Micheal,41 Dunwoody St,1994-07-29,50000.0)

unionMember.to[Employee].set(salary = 60000.0)
// res6: Employee = Employee(Micheal,41 Dunwoody St,1994-07-29,60000.0)
```
Missing fields will fail the compilation
```scala
case class People(name: String, address: String)
// defined class People

val people = People("John", "49 Wall St.")
// people: People = People(John,49 Wall St.)
```
```scala
scala> people.to[Employee]() //missing DoB
<console>:20: error: 
    You have not provided enough arguments to convert from People to Employee.
    shapeless.HNil

       people.to[Employee]() //missing DoB
                          ^
```
Wrong argument types will fail the compilation
```scala
scala> unionMember.to[Employee].set(salary = 60) //salary was input as Int rather than Double
<console>:20: error: One or more fields in shapeless.::[shapeless.labelled.FieldType[shapeless.tag.@@[Symbol,String("salary")],Int],shapeless.HNil] is not in Employee
error after rewriting to henkan.convert.Syntax.convert[UnionMember](unionMember).to[Employee].set.applyDynamicNamed("apply")(scala.Tuple2("salary", 60))
possible cause: maybe a wrong Dynamic method signature?
       unionMember.to[Employee].set(salary = 60) //salary was input as Int rather than Double
                                   ^
```


### Transform between case classes with optional field

`cats.optional` provides some facility to transform between case classes with optional fields and ones with required fields.
Suppose you have two case classes: `Message` whose fields are optional and `Domain` whose fields are required

```scala
case class Message(a: Option[String], b: Option[Int])
case class Domain(a: String, b: Int)
```
You can validate an instance of `Message` to a Validated `Domain`

```scala
import cats.data.Validated
import cats.implicits._
import henkan.optional.all._
```

```scala
validate(Message(Some("a"), Some(2))).to[Domain]
// res0: henkan.optional.ValidateFromOptional.Result[Domain] = Valid(Domain(a,2))

validate(Message(Some("a"), None)).to[Domain]
// res1: henkan.optional.ValidateFromOptional.Result[Domain] = Invalid(NonEmptyList(RequiredFieldMissing(b)))
```

The compilation will fail if the from case class doesn't have all fields the target case class needs
```scala

case class MessageWithMissingField(a: Option[String])
```

```scala
scala> validate(MessageWithMissingField(Some("a"))).to[Domain]
<console>:24: error: Cannot build validate function from MessageWithMissingField to Domain, possibly due to missing fields in MessageWithMissingField or missing cats instances (`Traverse` instances are needed to convert fields in containers)
       validate(MessageWithMissingField(Some("a"))).to[Domain]
                                                      ^
```

You can convert in the opposite direction as well
```scala
from(Domain("a", 2)).toOptional[Message]
// res4: Message = Message(Some(a),Some(2))
```

Note that if you from case class does not have all the fields the target class has, they will be set as `None`

```scala
case class DomainWithMissingField(a: String)
```
```scala
scala> from(DomainWithMissingField("a")).toOptional[Message]
res5: Message = Message(Some(a),None)
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
