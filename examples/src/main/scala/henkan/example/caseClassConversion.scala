package henkan.example

import java.time.LocalDate
import henkan.convert.Syntax._

object caseClassConversion {

  case class Employee(name: String, address: String, dateOfBirth: LocalDate, salary: Double = 50000d)

  case class UnionMember(name: String, address: String, dateOfBirth: LocalDate)

  case object Union {
    def notify(member: UnionMember): Unit = println(s"$member is notified")
  }

  case object Company {
    def pay(employee: Employee): Unit = println(s"$employee is payed with ${employee.salary}")
  }

  val employee = Employee("George", "123 E 86 St", LocalDate.of(1963, 3, 12), 54000)

  val unionMember = UnionMember("Micheal", "41 Dunwoody St", LocalDate.of(1994, 7, 29))

  //simple conversion
  Union.notify(employee.to[UnionMember]())

  //simple conversion with default value
  Company.pay(unionMember.to[Employee]())

  //conversion with different value
  Company.pay(unionMember.to[Employee].set(salary = 60000.0))

}
