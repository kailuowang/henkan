import henkan.exporter.ExporterSyntax
import henkan.extractor.ExtractorSyntax

package object henkan {
  type FieldName = String
  object syntax {
    object all extends ExtractorSyntax with ExporterSyntax
    object extract extends ExtractorSyntax
    object export extends ExporterSyntax
  }

}
