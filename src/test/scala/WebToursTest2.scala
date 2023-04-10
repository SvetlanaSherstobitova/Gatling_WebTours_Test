
import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import scala.util.Random

class WebToursTest2 extends Simulation {

	val domain = "localhost:1080"

	val httpProtocol = http
		.baseUrl("http://" + domain)

  val csvFeederLoginDetails = csv("data/loginDetails.csv").circular
  val departFeeder = csv("data/depart.csv").random

  object HomePage {
    def mainPage = {
      exec(http("Load Home Page")
        .get("/WebTours/")
        .check(status.is(200))
        .check(regex("<title>Web Tours</title>").exists)
        .resources(
          http("request_1")
            .get("/WebTours/header.html"),
          http("request_2")
            .get("/cgi-bin/welcome.pl?signOff=true"),
          http("Login Page")
            .get("/cgi-bin/nav.pl?in=home")
            .check(regex("""value=\"(.*?)\"""").exists.saveAs("SessionId")),
          http("request_4")
            .get("/WebTours/home.html")))
    }
  }

  object Customer {
    def login = {
      feed(csvFeederLoginDetails)
        .exec(
          http("Login Page")
            .get("/cgi-bin/nav.pl?in=home")
        )
        .exec(
          http("Customer Login Action")
            .post("/cgi-bin/login.pl")
            .formParam("userSession", "#{SessionId}")
            .formParam("username", "#{username}")
            .formParam("password", "#{password}")
            .formParam("login.x", "57")
            .formParam("login.y", "11")
            .formParam("JSFormSubmit", "off")
            .resources(
              http("request_6")
                .get("/cgi-bin/nav.pl?page=menu&in=home"),
              http("request_7")
                .get("/cgi-bin/login.pl?intro=true")
            )
        )
    }
  }

  object Flight {
    def view = {
      feed(departFeeder)
        .exec(
          http("Find Flight")
        .get("/cgi-bin/reservations.pl?page=welcome")
        )
    }

  }

  private val scn = scenario("WebToursTest2")
    .exec(HomePage.mainPage)
    .pause(2)
    .exec(Customer.login)
    .pause(2)
    .exec(
      http("request_8")
        .get("/cgi-bin/welcome.pl?page=search")
        .resources(
          http("request_9")
            .get("/cgi-bin/nav.pl?page=menu&in=flights"),
          http("Find Flight")
            .get("/cgi-bin/reservations.pl?page=welcome")
        )
    )
    .pause(2)
    .exec(
      http("request_11")
        .post("/cgi-bin/reservations.pl")
        .formParam("advanceDiscount", "0")
        .formParam("depart", "London")
        .formParam("departDate", "10/11/2023")
        .formParam("arrive", "Paris")
        .formParam("returnDate", "10/12/2023")
        .formParam("numPassengers", "1")
        .formParam("seatPref", "Window")
        .formParam("seatType", "Business")
        .formParam("findFlights.x", "42")
        .formParam("findFlights.y", "15")
        .formParam(".cgifields", "roundtrip")
        .formParam(".cgifields", "seatType")
        .formParam(".cgifields", "seatPref")
    )
    .pause(2)
    .exec(
      http("request_12")
        .post("/cgi-bin/reservations.pl")
        .formParam("outboundFlight", "243;129;10/11/2023")
        .formParam("numPassengers", "1")
        .formParam("advanceDiscount", "0")
        .formParam("seatType", "Business")
        .formParam("seatPref", "Window")
        .formParam("reserveFlights.x", "60")
        .formParam("reserveFlights.y", "9")
    )
    .pause(2)
    .exec(
      http("request_13")
        .post("/cgi-bin/reservations.pl")
        .formParam("firstName", "Jojo")
        .formParam("lastName", "Bean")
        .formParam("address1", "Pushkinskaya, 179")
        .formParam("address2", "Kazan")
        .formParam("pass1", "Jojo Bean")
        .formParam("creditCard", "987654321")
        .formParam("expDate", "10/24")
        .formParam("oldCCOption", "")
        .formParam("numPassengers", "1")
        .formParam("seatType", "Business")
        .formParam("seatPref", "Window")
        .formParam("outboundFlight", "243;129;10/11/2023")
        .formParam("advanceDiscount", "0")
        .formParam("returnFlight", "")
        .formParam("JSFormSubmit", "off")
        .formParam("buyFlights.x", "35")
        .formParam("buyFlights.y", "6")
        .formParam(".cgifields", "saveCC")
    )
    .pause(2)
    .exec(
      http("request_14")
        .get("/cgi-bin/welcome.pl?signOff=1")
        .resources(
          http("request_15")
            .get("/WebTours/home.html"),
          http("request_16")
            .get("/cgi-bin/nav.pl?in=home")
        )
    )

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
