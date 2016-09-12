package henkan.exporter

import algebra.{Semigroup, Monoid}
import henkan.FieldName
import shapeless.labelled.FieldType
import shapeless._, HList._
import cats.implicits._
import shapeless.ops.hlist.{LeftReducer, LeftFolder}

import scala.annotation.implicitNotFound

@implicitNotFound("Cannot find field writer of type ${T}, to ${S}")
trait FieldWriter[T, S] extends ((FieldName, T) ⇒ S)

object FieldWriter {
  def apply[T, S](f: (FieldName, T) ⇒ S) = new FieldWriter[T, S] {
    def apply(fn: FieldName, t: T) = f(fn, t)
  }
}

@implicitNotFound("For all fields in ${T} of type FT, there must be an implicit FieldWriter[FT, ${S}]")
trait Exporter[T, S] extends (T ⇒ S)

object Exporter {

  trait FieldExporter[T, S] extends ((FieldName, T) ⇒ S)

  object FieldExporter {
    implicit def fromWriter[T, S](implicit fieldWriter: FieldWriter[T, S]): FieldExporter[T, S] = new FieldExporter[T, S] {
      def apply(fn: FieldName, t: T): S = fieldWriter(fn, t)
    }

    implicit def recursiveFieldExporter[T, S](
      implicit
      exporter: Exporter[T, S],
      fieldWriter: FieldWriter[S, S]
    ): FieldExporter[T, S] = new FieldExporter[T, S] {
      def apply(fn: FieldName, t: T): S = {
        fieldWriter(fn, exporter(t))
      }
    }
  }

  object foldToS extends Poly2 {
    implicit def field[V1, K1 <: Symbol, V2, K2 <: Symbol, S: Semigroup](
      implicit
      key1: Witness.Aux[K1],
      key2: Witness.Aux[K2],
      fieldExporter1: FieldExporter[V1, S],
      fieldExporter2: FieldExporter[V2, S]
    ) = at[FieldType[K1, V1], FieldType[K2, V2]] { (v1: FieldType[K1, V1], v2: FieldType[K2, V2]) ⇒
      fieldExporter1(key1.value.name, v1) |+| fieldExporter2(key2.value.name, v2)
    }
  }

  implicit def mkExporter[T, S: Semigroup, Repr <: HList](
    implicit
    gen: LabelledGeneric.Aux[T, Repr],
    rdr: LeftReducer.Aux[Repr, foldToS.type, S]
  ): Exporter[T, S] = new Exporter[T, S] {
    def apply(t: T): S = {
      val repr = gen.to(t)
      repr.reduceLeft(foldToS)
    }
  }

}

trait ExporterSyntax {
  def export[T, S](t: T)(implicit exporter: Exporter[T, S]): S = exporter(t)
}

object ExporterSyntax extends ExporterSyntax
