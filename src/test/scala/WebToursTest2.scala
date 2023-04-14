
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.text.SimpleDateFormat
import java.time.LocalDate
import scala.util.Random

class WebToursTest2 extends Simulation {

	val domain = "localhost:1080"

	val httpProtocol = http
		.baseUrl("http://" + domain)

  val csvFeederLoginDetails = csv("data/loginDetails.csv").circular
  val csvDepartFeeder = csv("data/depart.csv").circular
  val csvCustomerFeeder = csv("data/customer.csv").circular
  val csvOutboundFlightFeeder = csv("data/outboundFlight1.csv").circular
  val inputFormat = new SimpleDateFormat("yyyy-MM-dd")
  val outputFormat = new SimpleDateFormat("MM/dd/yyyy")

  val start = LocalDate.of(2023, 5, 1)
  val end   = LocalDate.of(2023, 6, 29)

  val departDate = outputFormat.format(inputFormat.parse(LocalDate.ofEpochDay(Random.between(start.toEpochDay, end.toEpochDay)).toString))
  val returnDate = outputFormat.format(inputFormat.parse(LocalDate.ofEpochDay(Random.between(start.toEpochDay + 1, end.toEpochDay + 1)).toString))

  object HomePage {
    def mainPage = {
      exec(http("Load Home Page")
        .get("/WebTours/")
        .check(status.is(200))
        .check(regex("<title>Web Tours</title>").exists)
        .resources(
          http("Header Home Page")
            .get("/WebTours/header.html"),
          http("Sighoff True")
            .get("/cgi-bin/welcome.pl?signOff=true"),
          http("Login Page")
            .get("/cgi-bin/nav.pl?in=home")
            .check(regex("""name="userSession" value=\"(.*?)\"""").exists.saveAs("SessionId")),
          http("Home Page")
            .get("/WebTours/home.html")))
    }
  }
  object Customer {
    def login = {
      feed(csvFeederLoginDetails)
        .exec(http("Login Page")
            .get("/cgi-bin/nav.pl?in=home")
        )
        .exec(http("Customer Login Action")
            .post("/cgi-bin/login.pl")
            .formParam("userSession", "#{SessionId}")
            .formParam("username", "#{username}")
            .formParam("password", "#{password}")
            .formParam("login.x", "#{loginx}")
            .formParam("login.y", "#{loginy}")
            .formParam("JSFormSubmit", "off")
            .resources(
              http("Menu Home")
                .get("/cgi-bin/nav.pl?page=menu&in=home"),
              http("Login True")
                .get("/cgi-bin/login.pl?intro=true")
            )
        )
    }
  }
  object Flight {
    def view = {
      exec(http("Flights Page Open")
            .get("/cgi-bin/welcome.pl?page=search")
            .resources(
              http("Menu Flights")
                .get("/cgi-bin/nav.pl?page=menu&in=flights"),
              http("Find Flight")
                .get("/cgi-bin/reservations.pl?page=welcome")
            )
        )
    }
    def select = {
      feed(csvDepartFeeder)
        .exec(http("Reserve Flight")
          .post("/cgi-bin/reservations.pl")
          .check(
           substring("Flight").count.saveAs("FlightCount"),
            bodyString.saveAs("ReserveFlightResponse"),
            css("input[name='outboundFlight']", "value").saveAs("outboundFlight")

            // Working options!
            // css("input[name='outboundFlight']", "value").saveAs("outboundFlight")
            // regex("""name="outboundFlight" value=\"(.*?)\"""").exists.saveAs("outboundFlight"),
            // regex("name=\"outboundFlight\" value=\"(.*?)\"").exists.saveAs("outboundFlight")
          )

          .formParam("advanceDiscount", "0")
          .formParam("depart", "#{depart}")
          .formParam("departDate", departDate)
          .formParam("arrive", "#{arrive}")
          .formParam("returnDate", returnDate)
          .formParam("numPassengers", "#{numPassengers}")
          .formParam("seatPref", "#{seatPref}")
          .formParam("seatType", "#{seatType}")
          .formParam("findFlights.x", "#{findFlightsx}")
          .formParam("findFlights.y", "#{findFlightsy}")
          .formParam(".cgifields", "roundtrip")
          .formParam(".cgifields", "seatType")
          .formParam(".cgifields", "seatPref"))
//        .exec { session =>
//          println(" Flight: --> " + session("FlightCount").as[String])
//          println(" ReserveFlightResponse: --> " + session("ReserveFlightResponse").as[String])
//          session
//        }
    }
    def confirm = {
       exec(http("Confirm Flight")
          .post("/cgi-bin/reservations.pl")
          .formParam("outboundFlight", "#{outboundFlight}")
          .formParam("numPassengers", "#{numPassengers}")
          .formParam("advanceDiscount", "0")
          .formParam("seatType", "#{seatType}")
          .formParam("seatPref", "#{seatPref}")
          .formParam("reserveFlights.x", "#{reserveFlightsx}")
          .formParam("reserveFlights.y", "#{reserveFlightsy}"))
    }
    def pay = {
      feed(csvCustomerFeeder)
      .exec(http("Pay for Flight")
        .post("/cgi-bin/reservations.pl")
        .formParam("firstName", "#{firstName}")
        .formParam("lastName", "#{lastName}")
        .formParam("address1", "#{address1}")
        .formParam("address2", "#{address2}")
        .formParam("pass1", "#{pass1}")
        .formParam("creditCard", "#{creditCard}")
        .formParam("expDate", "#{expDate}")
        .formParam("oldCCOption", "")
        .formParam("numPassengers", "#{numPassengers}")
        .formParam("seatType", "#{seatType}")
        .formParam("seatPref", "#{seatPref}")
        .formParam("outboundFlight", "#{outboundFlight}")
        .formParam("advanceDiscount", "0")
        .formParam("returnFlight", "")
        .formParam("JSFormSubmit", "off")
        .formParam("buyFlights.x", "#{buyFlightsx}")
        .formParam("buyFlights.y", "#{buyFlightsy}")
        .formParam(".cgifields", "saveCC"))
    }
  }
  object Logout {
    def signOff = {
      exec(http("User SignOff")
        .get("/cgi-bin/welcome.pl?signOff=1")
        .resources(
          http("Login Page")
            .get("/cgi-bin/nav.pl?in=home"),
          http("Home Page")
            .get("/WebTours/home.html")
        )
      )
    }
  }

  val scn = scenario("WebToursTest2")
    .exec(HomePage.mainPage)
    .pause(2)
    .exec(Customer.login)
    .pause(2)
    .exec(Flight.view)
    .pause(2)
    .exec(Flight.select)
    .pause(2)
    .exec(Flight.confirm)
    .pause(2)
    .exec(Flight.pay)
    .pause(2)
    .exec(Logout.signOff)

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
