
import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import scala.util.Random

class WebToursTest extends Simulation {

  val domain = "localhost:1080"

  val httpProtocol = http
    .baseUrl("http://" + domain)

  val csvFeederLoginDetails = csv("data/loginDetails.csv").circular

  object HomePage {
    val homePage = exec(http("homePage_load")
      .get("/WebTours/")
      .resources(
        http("request_1")
          .get("/WebTours/header.html"),
        http("request_2")
          .get("/cgi-bin/welcome.pl?signOff=true"),
        http("request_3")
          .get("/cgi-bin/nav.pl?in=home")
          .check(regex("""value=\"(.*?)\"""").exists.saveAs("SessionId")),
        http("request_4")
          .get("/WebTours/home.html")
      )
    )
      .pause(2)
  }
  object Login {
    val login = exec(http("user_login")
      .post("/cgi-bin/login.pl")
      .formParam("userSession", "#{SessionId}")
      .formParam("username", "jojo")
      .formParam("password", "bean")
      .resources(
        http("findFlight_open")
          .get("/cgi-bin/nav.pl?page=menu&in=home"),
        http("request_7")
          .get("/cgi-bin/login.pl?intro=true")
      )
    )
      .pause(2)
  }
  object Reservations {
    val reservations = exec(http("findFlight_choose")
      .post("/cgi-bin/reservations.pl")
      .formParam("advanceDiscount", "0")
      .formParam("depart", "Los Angeles")
      .formParam("departDate", "04/09/2023")
      .formParam("arrive", "San Francisco")
      .formParam("returnDate", "04/10/2023")
      .formParam("numPassengers", "1")
      .formParam("roundtrip", "on")
      .formParam("seatPref", "Aisle")
      .formParam("seatType", "First")
      .formParam("findFlights.x", "71")
      .formParam("findFlights.y", "3")
      .formParam(".cgifields", "roundtrip")
      .formParam(".cgifields", "seatType")
      .formParam(".cgifields", "seatPref")
    )
      .pause(2)
  }

  object ReserveFlights {
    val reserveFlights = exec(http("findFlight_reserve")
      .post("/cgi-bin/reservations.pl")
      .formParam("outboundFlight", "360;113;04/09/2023")
      .formParam("returnFlight", "630;113;04/10/2023")
      .formParam("numPassengers", "1")
      .formParam("advanceDiscount", "0")
      .formParam("seatType", "First")
      .formParam("seatPref", "Aisle")
      .formParam("reserveFlights.x", "56")
      .formParam("reserveFlights.y", "11")
    )
      .pause(2)
  }

  object PaymentDetails {
    val paymentDetails = exec(http("paymentDetails_send")
      .post("/cgi-bin/reservations.pl")
      .formParam("firstName", "Jojo")
      .formParam("lastName", "Bean")
      .formParam("address1", "Lenina, 25")
      .formParam("address2", "Moscow")
      .formParam("pass1", "Jojo Bean")
      .formParam("creditCard", "12345678900000000000")
      .formParam("expDate", "10/25")
      .formParam("oldCCOption", "")
      .formParam("numPassengers", "1")
      .formParam("seatType", "First")
      .formParam("seatPref", "Aisle")
      .formParam("outboundFlight", "360;113;04/09/2023")
      .formParam("advanceDiscount", "0")
      .formParam("returnFlight", "630;113;04/10/2023")
      .formParam("JSFormSubmit", "off")
      .formParam("buyFlights.x", "28")
      .formParam("buyFlights.y", "4")
      .formParam(".cgifields", "saveCC")
    )
      .pause(2)
  }

  object SignOff {
    val signOff = exec(http("user_signOff")
      .get("/cgi-bin/welcome.pl?signOff=1")
      .resources(
        http("request_15")
          .get("/cgi-bin/nav.pl?in=home"),
        http("request_16")
          .get("/WebTours/home.html")
      )
    )
  }

  val scn = scenario("WebToursTest").exec(HomePage.homePage, Login.login, Reservations.reservations, ReserveFlights.reserveFlights, PaymentDetails.paymentDetails, SignOff.signOff)

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)

}
