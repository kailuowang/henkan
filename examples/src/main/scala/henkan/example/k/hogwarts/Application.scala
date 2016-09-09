package henkan.example.k.hogwarts

import henkan.example.k.ReasonableFuture
import henkan.example.k.hogwarts.Domain.{Wand, DormRoom, Mentor}
import henkan.example.k.hogwarts.Services.{GetWandRequest, AssignDormRoomRequest, AssignMentorRequest}

object Application extends App {
  import ReasonableFuture._
  import K._
  object mockServices extends Services {
    def wandService: ReasonableFuture.K[GetWandRequest, Wand] = K.pure(Wand("starlet", "ML2014"))

    def dormService: ReasonableFuture.K[AssignDormRoomRequest, DormRoom] = (req: AssignDormRoomRequest) ⇒
      if (req.name == "Mike") Result.left[DormRoom](UserError("Go away Mike")) else Result.pure(DormRoom("BlueStart", "230"))

    def mentorService: ReasonableFuture.K[AssignMentorRequest, Mentor] = K.pure(Mentor("Turing"))
  }

  val reg = Registration.register(mockServices)

  print(reg(Map("name" → "John", "address" → "32 Broadway", "sex" → "M", "specialty" → "Fire", "age" → "32")))

  def print[T](r: Result[T]): Unit = r.map(_.toString).recover { case r ⇒ r.toString }.map(println)
}
