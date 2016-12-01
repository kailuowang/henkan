package henkan

package object optional {

  object syntax {
    trait All extends ValidateFromOptionalSyntax with ToOptionalSyntax with FunctionSyntax
    object all extends All
    object fromOptional extends ValidateFromOptionalSyntax
    object toOptional extends ToOptionalSyntax
    object function extends FunctionSyntax
  }

  object seqInstance extends SeqInstance

  object all extends syntax.All with SeqInstance

}
