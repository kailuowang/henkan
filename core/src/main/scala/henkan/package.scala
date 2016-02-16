package object henkan {
  type FieldName = String
  object all extends ExtractorSyntax with ExporterSyntax with ConverterSyntax
  object extractor extends ExtractorSyntax
  object exporter extends ExporterSyntax
  object converter extends ConverterSyntax
}
