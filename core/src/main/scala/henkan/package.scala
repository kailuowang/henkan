import henkan.exporter.ExporterSyntax
import henkan.extractor.ExtractorSyntax

package object henkan {
  type FieldName = String
  object syntax {
    object all extends ExtractorSyntax with ExporterSyntax with ConverterSyntax
    object extract extends ExtractorSyntax
    object export extends ExporterSyntax
    object convert extends ConverterSyntax
  }

}
