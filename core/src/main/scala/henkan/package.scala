import henkan.exporter.ExporterSyntax
import henkan.extractor.ExtractorSyntax

package object henkan {
  type FieldName = String
  object all extends ExtractorSyntax with ExporterSyntax with ConverterSyntax
  object converter extends ConverterSyntax
}
