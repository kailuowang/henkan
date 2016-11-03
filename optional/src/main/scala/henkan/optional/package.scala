package henkan

package object optional {
  object syntax {
    object all extends ValidateFromOptionalSyntax with ToOptionalSyntax
    object fromOptional extends ValidateFromOptionalSyntax
    object toOptional extends ToOptionalSyntax
  }
}
