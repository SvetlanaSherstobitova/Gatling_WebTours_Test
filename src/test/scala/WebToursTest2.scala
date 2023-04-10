
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
  val csvDepartFeeder = csv("data/depart.csv").random
  val cvsSeatPrefFeeder = csv("data/seatingPreference.csv").random
  val csvSeatTypeFeeder = csv("data/seatType.csv").random
  val csvArriveFeeder = csv("data/arrive.csv").random
  val csvCustomerFeeder = csv("data/customer.csv").queue

  val rnd = new Random()

  def randomString(length: Int): String = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  val initSession = exec(flushCookieJar)
    .exec(session => session.set("randomNumber", rnd.nextInt))
    .exec(session => session.set("customerLoggedIn", false))
    .exec(addCookie(Cookie("sessionId", randomString(10)).withDomain(domain)))
    .exec { session => println(session); session }

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
        .exec(http("Login Page")
            .get("/cgi-bin/nav.pl?in=home")
        )
        .exec(http("Customer Login Action")
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
      exec(http("Flights Page Open")
            .get("/cgi-bin/welcome.pl?page=search")
            .resources(
              http("request_9")
                .get("/cgi-bin/nav.pl?page=menu&in=flights"),
              http("Find Flight")
                .get("/cgi-bin/reservations.pl?page=welcome")
                .check(regex("""option value=\"(.*?)\"""").exists.saveAs("city"))
                .check(regex("""seatPref\" value=\"(.*?)\"""").exists.saveAs("seatPref"))
                .check(regex("""seatType\" value=\"(.*?)\"""").exists.saveAs("seatType"))
            )
        )
    }
    def select = {
      exec(view).
      exec(http("Find Flight")
          //.get("/cgi-bin/nav.pl?page=menu&in=home")
          .get("/cgi-bin/reservations.pl")
        )
        .exec(http("Reserve Flight")
          .post("/cgi-bin/reservations.pl")
          .formParam("advanceDiscount", "0")
          .formParam("depart", "#{city}")
          .formParam("departDate", "10/11/2023")
          .formParam("arrive", "#{city}")
          .formParam("returnDate", "10/12/2023")
          .formParam("numPassengers", "1")
          .formParam("seatPref", "#{seatPref}")
          .formParam("seatType", "#{seatType}")
          .formParam("findFlights.x", "42")
          .formParam("findFlights.y", "15")
          .formParam(".cgifields", "roundtrip")
          .formParam(".cgifields", "seatType")
          .formParam(".cgifields", "seatPref"))
    }
    def confirm = {
    //  exec(http("Confirm Flight")
      //  .get("/cgi-bin/reservations.pl?page=welcome")
       // .check(regex("""outboundFlight\" value=\"\d\d\d;\d\d\d;\d\d\/\d\d\/\d\d\d\d\"""").exists.saveAs("outboundFlight"))
    //  )
      exec(
        http("Confirm Flight")
          .post("/cgi-bin/reservations.pl")
          .formParam("outboundFlight", "243;129;10/11/2023")
          .formParam("numPassengers", "1")
          .formParam("advanceDiscount", "0")
          .formParam("seatType", "#{seatType}")
          .formParam("seatPref", "#{seatPref}")
          .formParam("reserveFlights.x", "60")
          .formParam("reserveFlights.y", "9"))
    }
    def pay = {
      feed(csvCustomerFeeder)
      http("Pay for Flight")
        .post("/cgi-bin/reservations.pl")
        .formParam("firstName", "#{firstName}")
        .formParam("lastName", "#{lastName}")
        .formParam("address1", "Pushkinskaya 179")
        .formParam("address2", "Kazan")
        .formParam("pass1", "Jojo Bean")
        .formParam("creditCard", "987654321")
        .formParam("expDate", "10/24")
        .formParam("oldCCOption", "")
        .formParam("numPassengers", "1")
        .formParam("seatType", "#{seatType}")
        .formParam("seatPref", "#{seatPref}")
        .formParam("outboundFlight", "243;129;10/11/2023")
        .formParam("advanceDiscount", "0")
        .formParam("returnFlight", "")
        .formParam("JSFormSubmit", "off")
        .formParam("buyFlights.x", "35")
        .formParam("buyFlights.y", "6")
        .formParam(".cgifields", "saveCC")
    }
  }
  object Logout {
    def signOff = {
      exec(http("User SignOff")
        .get("/cgi-bin/welcome.pl?signOff=1")
        .resources(
          http("request_15")
            .get("/cgi-bin/nav.pl?in=home"),
          http("request_16")
            .get("/WebTours/home.html")
        )
      )
    }
  }

  val scn = scenario("WebToursTest2")
    .exec(initSession)
    .exec(HomePage.mainPage)
    .pause(2)
    .exec(Customer.login)
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
