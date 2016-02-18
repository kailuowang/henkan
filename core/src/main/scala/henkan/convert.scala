/**
 * Adapted from @joprice
 * https://gist.github.com/joprice/c9f9c42fe0e99c9ada87
 */

package henkan
import shapeless._, shapeless.ops.record._, shapeless.ops.hlist._

@annotation.implicitNotFound("""
    You have not provided enough arguments to convert from ${In} to ${Out}.
    ${Args}
                             """)
trait Converter[Args, In, Out] {
  def apply(args: Args, in: In): Out
}

object Converter {
  implicit def makeConvertible[Args <: HList, In, RIn <: HList, Out, Defaults <: HList, ROut <: HList, MD <: HList, MR <: HList, IR <: HList](
    implicit
    ingen: LabelledGeneric.Aux[In, RIn],
    outgen: LabelledGeneric.Aux[Out, ROut],
    defaults: Default.AsRecord.Aux[Out, Defaults],
    mergerD: Merger.Aux[RIn, Defaults, MD],
    mergerA: Merger.Aux[MD, Args, MR],
    intersection: Intersection.Aux[MR, ROut, IR],
    align: Align[IR, ROut]
  ): Converter[Args, In, Out] = new Converter[Args, In, Out] {
    def apply(args: Args, in: In) = {
      outgen.from(align(intersection(mergerA(mergerD(ingen.to(in), defaults()), args))))
    }
  }
}

trait ConverterSyntax {

  implicit class convert[InT <: Product](in: InT) {
    class ConvertTo[OutT] {
      object set extends RecordArgs {
        def applyRecord[R <: HList](rec: R)(
          implicit
          c: Converter[R, InT, OutT],
          checkFields: CheckFields[R, OutT]
        ): OutT = c(rec, in)
      }
      def apply()(implicit c: Converter[HNil, InT, OutT]): OutT = c(HNil, in)
    }
    def to[OutT] = new ConvertTo[OutT]
  }
}

object ConverterSyntax extends ConverterSyntax
