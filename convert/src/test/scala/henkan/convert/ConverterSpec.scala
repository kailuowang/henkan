package henkan.convert

import java.time.LocalDateTime

import henkan.convert.Syntax._
import org.specs2.mutable.Specification
import shapeless._, record._

import shapeless.test.illTyped

class ConverterSpec extends Specification {
  "convert to class with less fields" >> {
    case class Foo(bar: String, bar2: Int)
    case class Foo2(bar: String)

    val f = Foo("bR", 2)

    convert(f).to[Foo2]() === Foo2("bR")

  }

  "convert to class overriding fields" >> {

    case class Foo(bar: String, bar2: Int)
    case class Foo2(bar: String, bar2: Int)

    val f = Foo("bR", 2)

    convert(f).to[Foo2].set(bar2 = 3) === Foo2("bR", 3)
  }

  "convert to class with more fields" >> {
    case class Foo(bar: String)
    case class Foo2(bar: String, bar2: Int)

    val f = Foo("bR")

    f.to[Foo2].set(bar2 = 3) === Foo2("bR", 3)

  }

  "convert to class with more fields and default value" >> {
    case class Foo(bar: String)
    case class Foo2(bar: String, bar2: Int, bar3: Boolean = false)

    val f = Foo("bR")

    f.to[Foo2].set(bar2 = 3) === Foo2("bR", 3, false)
  }

  "convert to class with more fields and overriding default value" >> {
    case class Foo(bar: String)
    case class Foo2(bar: String, bar2: Int = 2)

    val f = Foo("bR")

    f.to[Foo2].set(bar2 = 3) === Foo2("bR", 3)
  }

  "only use default if not present" >> {
    case class Foo(bar: String)
    case class Foo2(bar: String = "def")
    val f = Foo("abc")
    f.to[Foo2]() === Foo2("abc")
  }

  "does not compile when missing fields" >> {
    case class Foo(bar: String)
    case class Foo2(bar: String, bar2: Int)
    val f = Foo("abc")
    illTyped { """ f.to[Foo2]() """ }
    f === f
  }

  "does not compile when missing fields" >> {
    case class Foo(bar: String)
    case class Foo2(bar: String, bar2: Int)
    val f = Foo("abc")
    illTyped { """ f.to[Foo2]() """ }
    f === f
  }

  "does not compile when setting the wrong fields" >> {
    case class Foo(bar: String)
    case class Foo2(bar: String, bar2: Int)
    val f = Foo("abc")
    illTyped { """ f.to[Foo2].set(bar3 = 3) """ }
    f.to[Foo2].set(bar2 = 3) === Foo2("abc", 3)
  }

  "does not compile when setting a default field with wrong type" >> {
    case class Foo(bar: String)
    case class Foo2(bar: String, bar2: Int = 2)
    val f = Foo("abc")
    illTyped { """ f.to[Foo2].set(bar3 = "3") """ }
    f.to[Foo2]() === Foo2("abc", 2)
  }

  "convert  when the field name is the same but value is of different type" >> {
    case class Foo(bar: String, bar2: String)
    case class Foo2(bar: String, bar2: Int)
    val f = Foo("abc", "efg")
    illTyped { """ f.to[Foo2]() """ }
    f.to[Foo2].set(bar2 = 2) === Foo2("abc", 2)
  }

  "compile with resonable time when setting the wrong fields with many fields" >> {
    case class Foo(
      bar1: String,
      bar2: Int,
      bar3: Boolean,
      bar4: LocalDateTime,
      bar5: List[String],
      bar6: Set[Boolean],
      bar7: Double,
      bar8: Long,
      bar9: Char,
      bar10: Float,
      bar11: String,
      bar12: Map[String, Int],
      bar13: Boolean,
      bar14: LocalDateTime,
      bar15: List[String],
      bar16: Set[Boolean],
      bar17: Double,
      bar18: Long,
      bar19: Char,
      bar20: Float
    )

    case class Foo2(
      bar1: String,
      bar2: Int,
      bar3: Boolean,
      bar4: LocalDateTime,
      bar5: List[String],
      bar6: Set[Boolean],
      bar7: Double,
      bar8: Long,
      bar9: Char,
      bar10: Float,
      bar11: String,
      bar12: Map[String, Int],
      bar13: Boolean,
      bar14: LocalDateTime,
      bar15: List[String],
      bar16: Set[Boolean],
      bar17: Double,
      bar18: Long,
      bar19: Char,
      bar20: Float,
      bar21: String
    )
    val now = LocalDateTime.now
    val f = Foo("abc", 3, true, now, Nil, Set.empty, 1d, 1l, '1', 1f, "dfd", Map(), true, now, Nil, Set.empty, 1d, 1l, '1', 1f)

    //this should compile
    illTyped { """ f.to[Foo2].set(bar21 = 4) """ }
    f.to[Foo2].set(bar21 = "big") === Foo2("abc", 3, true, now, Nil, Set.empty, 1d, 1l, '1', 1f, "dfd", Map(), true, now, Nil, Set.empty, 1d, 1l, '1', 1f, "big")
  }
}
