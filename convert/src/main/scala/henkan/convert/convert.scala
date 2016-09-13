/**
 * Adapted from @joprice
 * https://gist.github.com/joprice/c9f9c42fe0e99c9ada87
 */

package henkan.convert
import shapeless._

@annotation.implicitNotFound("""
    You have not provided enough arguments to convert from ${In} to ${Out}.
    ${Args}
                             """)
trait Converter[Args, In, Out] {
  def apply(args: Args, in: In): Out
}

object Converter {
  implicit def makeConvertible[Args <: HList, In, RIn <: HList, Out, Defaults <: HList, ROut <: HList](
    implicit
    ingen: LabelledGeneric.Aux[In, RIn],
    outgen: LabelledGeneric.Aux[Out, ROut],
    defaults: Default.AsRecord.Aux[Out, Defaults],
    build: Merge3[Args, RIn, Defaults, ROut]
  ): Converter[Args, In, Out] = new Converter[Args, In, Out] {
    def apply(args: Args, in: In) = {
      outgen.from(build(args, ingen.to(in), defaults()))
    }
  }
}

trait Syntax {

  implicit class convert[InT <: Product](in: InT) {
    class ConvertTo[OutT] {
      object set extends RecordArgs {
        def applyRecord[R <: HList](rec: R)(
          implicit
          checkFields: CheckFields[R, OutT],
          c: Converter[R, InT, OutT]
        ): OutT = c(rec, in)
      }
      def apply()(implicit c: Converter[HNil, InT, OutT]): OutT = c(HNil, in)
    }
    def to[OutT] = new ConvertTo[OutT]
  }
}

object Syntax extends Syntax
