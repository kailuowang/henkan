package henkan.example.k.hogwarts

import henkan.example.k.ReasonableFuture._
import henkan.convert.Syntax._
import K._
import ResultTransformations._

import henkan.example.k.hogwarts.Services.{AssignDormRoomRequest, AssignMentorRequest, GetWandRequest}
import Domain._
import henkan.extractor._

import scala.util.Try

object Domain {
  type Request = Map[String, String]

  case class BasicInfo(name: String, address: String, age: Int, sex: Sex)

  case class RegistrationRecord(basicInfo: BasicInfo, mentor: Mentor, dormRoom: DormRoom)

  sealed trait Sex
  case object Male extends Sex
  case object Female extends Sex

  sealed trait Specialty
  case object Fire extends Specialty
  case object Ice extends Specialty
  case object Air extends Specialty
  case object Earth extends Specialty

  case class Wand(brand: String, model: String)
  case class Mentor(name: String)
  case class DormRoom(building: String, number: String)

}

trait Services {
  def wandService: K[GetWandRequest, Wand]
  def mentorService: K[AssignMentorRequest, Mentor]
  def dormService: K[AssignDormRoomRequest, DormRoom]
}

object Services {
  case class GetWandRequest(name: String, address: String)
  case class AssignMentorRequest(wand: Wand, specialty: Specialty)
  case class AssignDormRoomRequest(name: String, sex: Sex, mentor: Mentor)
}

object Registration {
  import AutoExtractors._

  def register(services: Services): K[Request, RegistrationRecord] =
    for {
      bi ← baseInformationExtractor
      mentor ← services.mentorService.contraMapR(
        wand = pure[Request](bi.to[GetWandRequest]()) andThen services.wandService,
        specialty = stringExtractor("specialty") andThen toSpecialty
      )
      room ← pure[Request](bi.to[AssignDormRoomRequest].set(mentor = mentor)) andThen services.dormService
    } yield RegistrationRecord(bi, mentor, room)
}

trait RequestExtractor {

  def partialParseString[T](pf: PartialFunction[String, T]): K[String, T] =
    (a: String) ⇒
      pf.lift(a).toResult(UserError(s"Incorrect format $a"))

  def stringExtractor(key: String): K[Request, String] = K.of((req: Request) ⇒
    req.get(key).toResult(UserError(s"missing value of $key")))

  lazy val toSex: K[String, Sex] = partialParseString {
    case "F" ⇒ Female
    case "M" ⇒ Male
  }

  lazy val toSpecialty: K[String, Specialty] = partialParseString {
    case "Fire"  ⇒ Fire
    case "Ice"   ⇒ Ice
    case "Air"   ⇒ Air
    case "Earth" ⇒ Earth
  }

}

object ManualExtractor$ extends RequestExtractor {

  private lazy val ctbi = composeTo[BasicInfo]
  lazy val baseInformationParser: K[Request, BasicInfo] = ctbi(
    name = stringExtractor("name"),
    address = stringExtractor("address"),
    age = stringExtractor("age") andThen (a ⇒ Try(a.toInt).toResult()),
    sex = stringExtractor("sex") andThen toSex
  )

}

object AutoExtractors extends RequestExtractor {

  implicit val frString: FieldReader[Result, Request, String] = stringExtractor _

  implicit val frInt: FieldReaderMapper[String, Result[Int]] =
    (s: String) ⇒ Try(s.toInt).toResult()

  implicit val frSex: FieldReaderMapper[String, Result[Sex]] =
    toSex.run

  lazy val baseInformationExtractor: K[Request, BasicInfo] = Extractor[Result, Request, BasicInfo].apply()

}

