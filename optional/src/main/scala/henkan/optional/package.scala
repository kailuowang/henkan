package henkan

package object optional {
  object syntax {
    object all extends ValidateFromOptionalSyntax with ToOptionalSyntax with FunctionSyntax
    object fromOptional extends ValidateFromOptionalSyntax
    object toOptional extends ToOptionalSyntax
    object function extends FunctionSyntax
  }
}
