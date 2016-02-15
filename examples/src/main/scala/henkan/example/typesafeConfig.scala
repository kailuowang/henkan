package henkan.example

import java.time.Duration

import henkan.{Extractor, FieldReader}
import henkan.extractor._
import com.typesafe.config._
import cats.implicits._
import alleycats.std.OptionInstances._

object TypesafeConfig {

  /**
   * trivial implementation of a typesafe config to case class library
   */
  object Lib {

    def reader[T](f: (Config, String) ⇒ T): FieldReader[Option, Config, T] = FieldReader { (cfg, fieldName) ⇒
      if (cfg.hasPath(fieldName)) Option(f(cfg, fieldName))
      else None
    }

    implicit val rInt = reader(_.getInt(_))
    implicit val rDouble = reader(_.getDouble(_))
    implicit val rString = reader(_.getString(_))
    implicit val rLong = reader(_.getLong(_))
    implicit val rDuration = reader(_.getDuration(_))
    implicit val rBoolean = reader(_.getBoolean(_))
    implicit val rConfig = reader(_.getConfig(_))

  }

  object ExampleUsage {

    import Lib._

    case class FarmSettings(
      name: String,
      barnHouse: BarnHouseSettings,
      numOfAnimals: Int
    )

    case class BarnHouseSettings(
      footage: Double,
      openHours: Duration,
      parkTractor: Boolean = false
    )

    val cfgString =
      """
        | name: "Old McDonald"
        | numOfAnimals: 5
        | barnHouse {
        |   footage: 2045.5
        |   openHours: 8h
        | }
      """.stripMargin

    val cfg = ConfigFactory.parseString(cfgString)

    val result: Option[FarmSettings] = extract[Option, FarmSettings](cfg) //

    assert(result.contains(
      FarmSettings(
        "Old McDonald",
        BarnHouseSettings(
          2045.5d,
          Duration.ofHours(8),
          false
        ),
        5
      )
    ))
  }
}
