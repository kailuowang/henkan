package henkan.example.extract

import java.time.Duration

import com.typesafe.config._
import henkan.extractor._
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

    case class FarmSettings(
      name: String,
      barnHouse: BarnHouseSettings,
      numOfAnimals: Int
    )
    val result: Option[FarmSettings] = extract[Option, FarmSettings](cfg)

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

    case class FarmSettings2(
      name: String,
      numOfAnimals: Int,
      barnHouse: BarnHouseSettings
    )

    //todo: this line shouldn't be needed, fix github issue #15 to fix this
    implicit val eb = Extractor[Option, Config, BarnHouseSettings]

    val result2: Option[FarmSettings2] = extract[Option, FarmSettings2](cfg)

    assert(result2.contains(
      FarmSettings2(
        "Old McDonald",
        5,
        BarnHouseSettings(
          2045.5d,
          Duration.ofHours(8),
          false
        )
      )
    ))
  }
}
