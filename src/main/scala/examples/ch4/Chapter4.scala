package examples.ch4

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import monix.execution.Scheduler.Implicits.global
import monix.execution.Ack.Continue
import monix.execution.Cancelable
import monix.reactive.Observable

class Person {}

class PersonDao {

  private val PAGE_SIZE = 10

  def listPeople(): List[Person] = query("SELECT * FROM PEOPLE")

  def listPeople2: Observable[Person] = {
    val people = query("SELECT * FROM PEOPLE")
    Observable.fromIterable(people)
  }

  def listPeople3: Observable[Person] =
    Observable.defer(Observable.fromIterable(query("SELECT * FROM PEOPLE")))

  def listPeoplePaged(initialPage: Long): List[Person] =
    query("SELECT * FROM PEOPLE OFFSET ? MAX ?",
      PAGE_SIZE,
      initialPage * PAGE_SIZE)

  // can SO
  def allPeople(initialPage: Int): Observable[Person] =
    Observable.defer(Observable.fromIterable(listPeoplePaged(initialPage))) ++
      Observable.defer(allPeople(initialPage + 1))

  val allPages: Observable[Person] =
    Observable.range(0, Integer.MAX_VALUE)
      .map(listPeoplePaged)
      .takeWhile(_.nonEmpty)
      .concatMap(Observable.fromIterable)

  private def query(sql: String, args: Any*) = //...
    List(new Person(), new Person())

}

class Book {
  def getTitle = "Title"
}

object Chapter4 {

  private val personDao = new PersonDao
  //  private val orderBookLength = 0

  private def marshal(people: List[Person]) = people.toString

  { // 9
    val people = personDao.listPeople()
    val json = marshal(people)
  }

  { // 20 + 34
    val peopleStream = personDao.listPeople2
    val peopleList = peopleStream.toListL
    val people = Await.result(peopleList.runAsync, Duration.Inf) // there's no BlockingObservable, you just have to block on Task
  }

  { // books

    def bestBookFor(person: Person): Unit = {
      val book =
        try
          recommend(person)
        catch {
          case _: Exception => bestSeller
        }
      display(book.getTitle)
    }

    def recommend(person: Person) = new Book

    def bestSeller = new Book

    def recommend2(person: Person) = Observable.eval(new Book)

    def bestSeller2 = Observable.eval(new Book)

    def display(title: String): Unit = {
      //...
    }

    def bestBookFor2(person: Person): Cancelable = {
      val recommended = recommend2(person)
      val bestSeller = bestSeller2
      val book = recommended.onErrorFallbackTo(bestSeller)
      val title = book.map(_.getTitle)
      title.subscribe { t =>
        display(t)
        Continue
      }
    }

    def bestBookFor3(person: Person) =
      recommend2(person)
        .onErrorFallbackTo(bestSeller2)
        .map(_.getTitle)
        .subscribe { t =>
          display(t)
          Continue
        }

  }

}
