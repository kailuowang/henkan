package henkan

trait Reader[S, F[_], T] {
  def read(source: S): F[T]
}

trait FieldReader[S, F[_], T] {
  def read(source: S, fieldName: String): F[T]
}
