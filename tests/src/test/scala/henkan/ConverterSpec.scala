package henkan

import henkan.syntax.convert._
import org.specs2.mutable.Specification
import shapeless.ops.record.{RemoveAll, Keys, SelectAll}

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
}
