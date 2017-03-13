package examples
package ch3

import java.time.{DayOfWeek, LocalDate}
import java.util.UUID

import scala.concurrent.duration._

import monix.execution.Ack.Continue
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global

import twitter4j.Status

import util.observable._
import util.time._

class CarPhoto {}
class LicensePlate {}

class Order {}

class Customer {
  def getOrders = List(new Order, new Order)
}

class Rating {}
class Profile {}

class User {
  def loadProfile = {
    //Make HTTP request...
    Observable(new Profile)
  }
}

trait WeatherStation {
  def temperature: Observable[Temperature]
  def wind: Observable[Wind]
}

class BasicWeatherStation extends WeatherStation {
  def temperature: Observable[Temperature] = Observable(new Temperature)
  def wind: Observable[Wind] = Observable(new Wind)
}

class Temperature {}
class Wind {}

class Weather(temperature: Temperature, wind: Wind) {
  def isSunny = true
  def getTemperature = temperature
}

sealed trait City
case object Warsaw extends City
case object London extends City
case object Paris extends City
case object NewYork extends City

class Flight {}
class Hotel {}

class Vacation(where: City, when: LocalDate) {
  def weather = Observable(new Weather(new Temperature, new Wind))
  def cheapFlightFrom(from: City) = Observable(new Flight)
  def cheapHotel = Observable(new Hotel)
}

object Chapter3 extends App {

  {
    // 6 + 15
    val strings = Observable.empty[String]
    val filtered = strings.filter(_.startsWith("#"))
    val instructions = strings.filter(_.startsWith(">"))
    val empty = strings.filter(_.isEmpty)
  }


  {
    // 26 + 49
    val tweets = Observable.empty[Status]
    val instants = tweets.map(_.getCreatedAt).map(_.toInstant)
  }

  {
    // 57 + 69
    Observable(8, 9, 10)
      .doOnNext(i => println(s"A: $i"))
      .filter(_ % 3 > 0)
      .doOnNext(i => println(s"B: $i"))
      .map(i => s"#${i * 10}")
      .doOnNext(s => println(s"C: $s"))
      .filter(_.length < 4)
      .subscribe { s =>
        println(s"D: $s")
        Continue
      }
  }

  {
    // 79
    val numbers = Observable(1, 2, 3, 4)
    numbers.map(_ * 2)
    numbers.filter(_ != 10)
    //equivalent

    // NOTE this doesn't change anything here, but in Monix flatMap == concatMap, not mergeMap
    numbers.flatMap(x => Observable(x * 2))
    numbers.flatMap(x => if (x != 10) Observable(x) else Observable.empty)
  }

  {
    // 100
    def recognize(photo: CarPhoto) = Observable(new LicensePlate)

    val cars = Observable(new CarPhoto)
    val plates = cars.map(recognize)
    val plates2 = cars.flatMap(recognize)
  }

  {
    // 111 + 119 + 127
    val customers = Observable(new Customer)
    val orders = customers.flatMap(customer => Observable.fromIterable(customer.getOrders))
    val orders2 = customers.map(_.getOrders).flatMap(Observable.fromIterable)
    // there's no `flatMapIterable` in Monix
  }

  {
    // 155
    def upload(id: UUID) = Observable(42L)

    def rate(id: UUID) = Observable(new Rating)

    val id = UUID.randomUUID
    // Monix doesn't have that version of `flatMap`
    // in this example we can use `completed` to ignore everything and `concat` the next stage
    upload(id).completed ++ rate(id)
  }

  {
    //213
    sealed trait Sound
    case object DI extends Sound
    case object DAH extends Sound

    def toMorseCode(ch: Char) =
      ch match {
        case 'a' => Observable(DI, DAH)
        case 'b' => Observable(DAH, DI, DI, DI)
        case 'c' => Observable(DAH, DI, DAH, DI)
        case 'd' => Observable(DAH, DI, DI)
        case 'e' => Observable(DI)
        case 'f' => Observable(DI, DI, DAH, DI)
        case 'g' => Observable(DAH, DAH, DI)
        case 'h' => Observable(DI, DI, DI, DI)
        case 'i' => Observable(DI, DI)
        case 'j' => Observable(DI, DAH, DAH, DAH)
        case 'k' => Observable(DAH, DI, DAH)
        case 'l' => Observable(DI, DAH, DI, DI)
        case 'm' => Observable(DAH, DAH)
        case 'n' => Observable(DAH, DI)
        case 'o' => Observable(DAH, DAH, DAH)
        case 'p' => Observable(DI, DAH, DAH, DI)
        case 'q' => Observable(DAH, DAH, DI, DAH)
        case 'r' => Observable(DI, DAH, DI)
        case 's' => Observable(DI, DI, DI)
        case 't' => Observable(DAH)
        case 'u' => Observable(DI, DI, DAH)
        case 'v' => Observable(DI, DI, DI, DAH)
        case 'w' => Observable(DI, DAH, DAH)
        case 'x' => Observable(DAH, DI, DI, DAH)
        case 'y' => Observable(DAH, DI, DAH, DAH)
        case 'z' => Observable(DAH, DAH, DI, DI)
        case '0' => Observable(DAH, DAH, DAH, DAH, DAH)
        case '1' => Observable(DI, DAH, DAH, DAH, DAH)
        case '2' => Observable(DI, DI, DAH, DAH, DAH)
        case '3' => Observable(DI, DI, DI, DAH, DAH)
        case '4' => Observable(DI, DI, DI, DI, DAH)
        case '5' => Observable(DI, DI, DI, DI, DI)
        case '6' => Observable(DAH, DI, DI, DI, DI)
        case '7' => Observable(DAH, DAH, DI, DI, DI)
        case '8' => Observable(DAH, DAH, DAH, DI, DI)
        case '9' => Observable(DAH, DAH, DAH, DAH, DI)
        case _ => Observable.empty
      }

    Observable('S', 'p', 'a', 'r', 't', 'a').map(_.toLower).flatMap(toMorseCode)
  }

  println("---------")

  {
    // 218
    // there's no such version of `delay` in Monix, so we skip to the second version
    // again, note that `flatMap` in Monix is deterministic, so we use `mergeMap` directly

    Observable("Lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit")
      .mergeMap(word => Observable.evalDelayed(word.length.seconds, word))
      .subscribePrintln()
    sleep(15.seconds)
  }

  println("---------")

  {
    // 249
    def loadRecordsFor(dow: DayOfWeek) =
      dow match {
        case DayOfWeek.SUNDAY =>
          Observable.interval(90.milliseconds).take(5).map(i => s"Sun-$i")
        case DayOfWeek.MONDAY =>
          Observable.interval(65.milliseconds).take(5).map(i => s"Mon-$i")
        case _ =>
          throw new IllegalArgumentException(s"Illegal: $dow")
      }

    Observable(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)
      .mergeMap(loadRecordsFor)
      .subscribePrintln() // the output here is a bit scrambled, e.g. Sun-0 comes before Mon-0
    sleep(1.seconds)

    println("---")

    Observable(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)
      .concatMap(loadRecordsFor)
      .subscribePrintln() // the output here is a bit scrambled, e.g. Sun-0 comes before Mon-0
    sleep(1.seconds)
  }

  {
    // 258
    val veryLargeList = List(new User, new User, new User, new User)
    val profiles = Observable.fromIterable(veryLargeList).mergeMap(_.loadProfile)
    // instead of `maxConcurrent` you can be more flexible with `OverflowStrategy` in Monix
  }

  {
    // 317
    def fastAlgo(photo: CarPhoto) = {
      //Fast but poor quality
      Observable(new LicensePlate)
    }

    def preciseAlgo(photo: CarPhoto) = {
      //Precise but can be expensive
      Observable(new LicensePlate)
    }

    def experimentalAlgo(photo: CarPhoto) = {
      //Unpredictable, running anyway
      Observable(new LicensePlate)
    }

    val photo = new CarPhoto
    val all = Observable.merge(preciseAlgo(photo), fastAlgo(photo), experimentalAlgo(photo)) // `merge` is just `mergeMap(identity)`
  }

  {
    // 286
    val station = new BasicWeatherStation
    val temperatureMeasurements = station.temperature
    val windMeasurements = station.wind
    temperatureMeasurements.zipMap(windMeasurements)(new Weather(_, _))
  }

  {
    // 298
    val oneToEight = Observable.range(1, 8)
    val ranks = oneToEight.map(_.toString)
    val files = oneToEight.map(x => 'a' + x - 1).map(_.toChar.toString) // don't need `intValue`, Char + number = number
    val squares = files.flatMap(file => ranks.map(_ concat file)) // can be replaced w/ for-comprehension
  }

  {
    // 312
    val nextTenDays: Observable[LocalDate] = Observable.range(1, 10).map(i => LocalDate.now().plusDays(i))
    val possibleVacations =
      Observable(Warsaw, London, Paris).flatMap(city =>
        nextTenDays
          .map(date => new Vacation(city, date))
          .flatMap(vacation =>
            Observable.zipMap3(
              vacation.weather.filter(_.isSunny),
              vacation.cheapFlightFrom(NewYork),
              vacation.cheapHotel
            )((_, _, _) => vacation)
          )
      )
  }

  println("---------")

  {
    // 332
    val red = Observable.interval(10.millis).map(_ => System.currentTimeMillis())
    val green = Observable.interval(10.millis).map(_ => System.currentTimeMillis())
    val c = Observable
      .zipMap2(red, green)((r, g) => r - g)
      .subscribePrintln()
    sleep(1.seconds)
    c.cancel()
  }

  println("---------")


  {
    // 345
    val c = Observable
      .combineLatestMap2(
        Observable.interval(17.millis).map(x => s"S$x"),
        Observable.interval(10.millis).map(x => s"F$x")
      )((s, f) => s"$f:$s")
      .subscribePrintln()
    sleep(2.seconds)
    c.cancel()
  }

  println("---------")

  {
    // 355
    val fast = Observable.interval(10.millis).map(x => s"F$x")
      .delaySubscription(100.millis).startWith(Seq("FX"))   // need the explicit Seq, otherwise String = Seq[Char]
    val slow = Observable.interval(17.millis).map(x => s"S$x")
    val c = slow
      .withLatestFrom(fast)((s, f) => s"$f:$s")
      .subscribePrintln()
    sleep(1.seconds)
    c.cancel()
  }

}
