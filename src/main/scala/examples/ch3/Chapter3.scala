package examples
package ch3

import java.time.{DayOfWeek, LocalDate}
import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

import monix.execution.Ack.{Continue, Stop}
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global
import monix.reactive.observables.GroupedObservable
import monix.reactive.observables.ObservableLike.{Operator, Transformer}
import monix.reactive.observers.Subscriber

import twitter4j.Status

import util.log._
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
  def loadProfile: Observable[Profile] = {
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
  def getTemperature: Temperature = temperature
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

class CashTransfer {
  def getAmount = BigDecimal(1)
}

class Data {}

class Car
object Car {
  def loadFromDb = Observable(new Car)
  def loadFromCache = Observable(new Car)
}

class ReservationEvent {
  final private val uuid = UUID.randomUUID
  def getReservationUuid: UUID = uuid
}

class Reservation {
  def consume(event: ReservationEvent): Reservation = { //mutate myself
    this
  }
}

trait FactStore {
  def observe: Observable[ReservationEvent]
}

class CassandraFactStore extends FactStore {
  override def observe: Observable[ReservationEvent] = Observable(new ReservationEvent)
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
    val oneToEight = Observable.range(1, 9)
    val ranks = oneToEight.map(_.toString)
    val files = oneToEight.map(x => 'a' + x - 1).map(_.toChar.toString) // don't need `intValue`, Char + number = number
    val squares = files.flatMap(file => ranks.map(_ concat file)) // can be replaced w/ for-comprehension
  }

  {
    // 312
    val nextTenDays = Observable.range(1, 11).map(i => LocalDate.now().plusDays(i))
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
      .delaySubscription(100.millis).startWith(Seq("FX")) // need the explicit Seq, otherwise String = Seq[Char]
    val slow = Observable.interval(17.millis).map(x => s"S$x")
    val c = slow
      .withLatestFrom(fast)((s, f) => s"$f:$s")
      .subscribePrintln()
    sleep(1.seconds)
    c.cancel()
  }

  println("---------")

  {
    // 367
    Observable(1, 2)
      .startWith(Seq(0)) // as seen before, `startWith` in Monix takes a `Seq`
      .subscribePrintln()
  }

  println("---------")

  def stream(initialDelayMs: Int, intervalMs: Int, name: String): Observable[String] =
    Observable.intervalWithFixedDelay(initialDelayMs.millis, intervalMs.millis)
      .map(x => s"$name$x")
      .doOnSubscribe(() =>
        logTs(s"Subscribe to $name")
      )
      .doOnSubscriptionCancel(() =>
        logTs(s"Unsubscribe from $name")
      )

  {
    // 375
    val c = Observable.firstStartedOf(stream(100, 17, "S"), stream(200, 10, "F"))
      .subscribeLog()
    sleep(1.seconds)
    c.cancel()
  }

  println("---------")

  {
    // 393
    val c = stream(100, 17, "S")
      .ambWith(stream(200, 10, "F"))
      .subscribeLog()
    sleep(1.seconds)
    c.cancel()
  }

  println("---------")

  def transferFile: Observable[Long] =
    Observable.interval(500.millis).map(_ => Random.nextLong().abs % 20 + 10).take(100)

  { // 419
    val progress = transferFile
    val totalProgress = progress
      .scan(0L)((total: Long, chunk: Long) => total + chunk) // you always have to provide the initial value
    val c = totalProgress.subscribePrintln()
    sleep(3.seconds)
    c.cancel()
  }

  println("---------")

  { // 431
    val factorials = Observable
      .range(2, 100)
      .scan(BigInt(1))((big, cur) => big * BigInt(cur)) // `scan` in Monix does not emit initial value
  val c = factorials.subscribePrintln()
    sleep(10.millis)
    c.cancel()
  }

  println("---------")

  { // 440
    val transfers = Observable(new CashTransfer)

    val total1 = transfers // `reduce` does not typecheck here, need to use a fold
      .foldLeftF(BigDecimal(0))((totalSoFar, transfer) => totalSoFar + transfer.getAmount)

    val total2 = transfers // this one won't produce anything, seems that `reduce` emits for 2+ elements
      .map(_.getAmount).reduce(_ + _)

  }

  // Monix doesn't have `collect` - just use `foldLeftF`, preferably without mutable accums
  { // 456
    val all = Observable.range(10, 20).foldLeftF(new ListBuffer[Long]) { (list, item) =>
      list.append(item)
      list
    }
  }


  // 463 + 470
  // still no `collect`

  def randomInts: Observable[Int] =
    Observable.unsafeCreate[Int] { s =>
      val random = new Random
      while (s.onNext(random.nextInt(1000)) != Stop) ()
      Cancelable(() => s.onComplete())
    }

  { // 490
    val uniqueRandomInts = randomInts.distinct.take(10)
  }

  { // 499
    val tweets = Observable.empty[Status]
    val distinctUserIds = tweets.map(_.getUser.getId).distinct
  }

  { // 508
    val tweets = Observable.empty[Status]
    val distinctUserIds = tweets.distinctByKey(_.getUser.getId)
  }

  { // 516
    val measurements = Observable.empty[Weather]
    val tempChanges = measurements.distinctUntilChangedByKey(_.getTemperature)
  }

  { // 524
    Observable.range(1, 6).take(3) // [1, 2, 3]
    Observable.range(1, 6).drop(3) // [4, 5]       // not `skip`
    Observable.range(1, 6).drop(5) // []

  }

  { // 531
    Observable.range(1, 6).takeLast(2)
    Observable.range(1, 6).dropLast(2) // not `skipLast`
  }

  { // 537
    // `takeUntil` in Monix has different semantics
    Observable.range(1, 6).takeWhile(_ != 3) // [1, 2]

  }

  { // 543
    val size = Observable('A', 'B', 'C', 'D')
      .foldLeftL(0)((sizeSoFar, _) => sizeSoFar + 1)
  }

  { // 550
    val numbers = Observable.range(1, 6)
    numbers.forAllF(_ != 4) // [false]
    numbers.existsF(_ == 4) // [true]
    // there's no `contains`
  }

  { // 559
    val veryLong = Observable.range(0, 1001).map(_ => new Data)
    val ends = Observable.concat(veryLong.take(5), veryLong.takeLast(5))
  }

  { // 570
    val fromCache = Car.loadFromCache
    val fromDb = Car.loadFromDb
    val found = Observable.concat(fromCache, fromDb).headF
  }

  def speak(quote: String, millisPerChar: Long): Observable[String] = {
    val tokens = quote.replaceAll("[:,]", "").split(" ")
    val words = Observable.fromIterable(tokens)
    val absoluteDelay = words.map(_.length).map(_ * millisPerChar).scan(0L)(_ + _)
    words.zip(absoluteDelay.startWith(Seq(0L)))
      .mergeMap(pair => Observable(pair._1).delaySubscription(pair._2.millis)) // `flatMap` == `concatMap` in Monix
      .doOnSubscribe(() =>
      println(s"Subscribe to ${quote.take(6)}")
    )
      .doOnSubscriptionCancel(() =>
        println(s"Unsubscribe from ${quote.take(6)}")
      )
  }

  println("---------")

  { // 28
    val alice = speak("To be, or not to be: that is the question", 110)
    val bob = speak("Though this be madness, yet there is method in't", 90)
    val jane = speak("There are more things in Heaven and Earth, Horatio, than are dreamt of in your philosophy", 100)
    Observable.merge(
      alice.map(w => s"Alice: $w"),
      bob.map(w => s"Bob:   $w"),
      jane.map(w => s"Jane:  $w")
    ).subscribePrintln()
    sleep(10.seconds)
  }

  println("---------")

  { // 52

    val timeout = 10.seconds

    val alice = speak("To be, or not to be: that is the question", 110)
    val bob = speak("Though this be madness, yet there is method in't", 90)
    val jane = speak("There are more things in Heaven and Earth, Horatio, than are dreamt of in your philosophy", 100)
    val rnd = new Random
    Observable(
      alice.map(w => s"Alice: $w"),
      bob.map(w => s"Bob:   $w"),
      jane.map(w => s"Jane:  $w")
    ).mergeMap(innerObs => Observable(innerObs).delaySubscription(rnd.nextInt(5).seconds))
      .delayOnComplete(timeout) // we have to add this, or else the child will be cancelled as soon as the parent finishes emitting
      .switch
      .dump("")
      .subscribe()

    sleep(timeout)
  }

  def store(id: UUID, modified: Reservation) = {
    //...
  }

  def loadBy(uuid: UUID) = { //...
    Option(new Reservation)
  }

  def updateProjection(event: ReservationEvent): Ack = {
    val uuid = event.getReservationUuid
    val res = loadBy(uuid).getOrElse(new Reservation)
    res.consume(event)
    store(event.getReservationUuid, res)
    Continue
  }

  { // 9
    val factStore = new CassandraFactStore
    val facts = factStore.observe
    facts.subscribe(updateProjection _)
  }

  def updateProjectionAsync(event: ReservationEvent) = { //possibly asynchronous
    Observable(new ReservationEvent)
  }

  { // 34
    val factStore = new CassandraFactStore
    val facts = factStore.observe
    facts.flatMap(updateProjectionAsync).subscribe
    //...
  }

  { // 52
    val factStore = new CassandraFactStore
    val facts = factStore.observe
    val grouped = facts.groupBy(_.getReservationUuid)
    grouped.subscribe { byUuid: GroupedObservable[UUID, ReservationEvent] =>
      byUuid.subscribe(updateProjection _)
      Continue
    }
  }

  { // 589
    val trueFalse = Observable(true, false).repeat
    val upstream = Observable.range(30, 38)
    val downstream = upstream
      .zip(trueFalse)
      .filter(_._2)
      .map(_._1)
  }

  { // 600
    val trueFalse = Observable(true, false).repeat
    val upstream = Observable.range(30, 38)
    upstream.zipMap(trueFalse)((t: Long, bool: Boolean) =>
      if (bool) Observable(t) else Observable.empty
    ).flatten
  }

  def odd[T]: Transformer[T, T] = {
    val trueFalse = Observable(true, false).repeat
    _.zip(trueFalse)
     .filter(_._2)
     .map(_._1)
  }

  println("---------")

  { // 618
    //[A, B, C, D, E...]
    val alphabet = Observable.range(0, 'Z' - 'A' + 1).map(c => ('A' + c).toChar)
    //[A, C, E, G, I...]
    alphabet.transform(odd).subscribePrintln() // `compose` => `transform`
  }

  println("---------")

  { // 9
    Observable
      .range(1, 1000)
      .filter(_ % 3 == 0)
      .distinct
      .reduce((a, x) => a + x)
      .map(_.toHexString)
      .subscribePrintln()
  }

  // see the implementation @ monix.reactive.internal.operators.MapOperator

  def toStringOfOdd[T] = new Operator[T, String]() {
    private var odd = true

    def apply(child: Subscriber[String]): Subscriber[T] = new Subscriber[T] {
      implicit val scheduler: Scheduler = child.scheduler
      private[this] var isDone = false

      def onNext(t: T): Future[Ack] = {
        val res = if (odd)
          child.onNext(t.toString)
        else
          Continue // there's no `request`, you need to `Continue` and keep track manually
        odd = !odd
        res
      }

      def onError(ex: Throwable): Unit =
        if (!isDone) {
          isDone = true
          child.onError(ex)
        }

      def onComplete(): Unit =
        if (!isDone) {
          isDone = true
          child.onComplete()
        }

    }
  }

  { // 59
    val odd = Observable
      .range(1, 10)
      .liftByOperator(toStringOfOdd)
    //Will emit: "1", "3", "5", "7" and "9" strings
  }

  { // 67
    Observable
      .range(1, 10)
      .bufferSliding(1, 2)
      .concatMap(Observable.fromIterable)
      .map(_.toString)
  }

  println("---------")

  { // 112
    Observable
      .range(1, 5).repeat
      .liftByOperator(toStringOfOdd)
      .take(3).subscribe({ x =>
      println(x)
      Continue
    }, _.printStackTrace, () => println("Completed")
    )
  }

  // the part about not passing the child is not relevant because there is no `unsubscribe`

}