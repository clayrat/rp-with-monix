package examples.ch1

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import scala.concurrent.duration._
import scala.concurrent.Future
import monix.eval.Task
import monix.execution.Ack.Continue
import monix.execution.{Cancelable, Scheduler}
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

object Chapter1 extends App {

  implicit class PrinterObservable[A](val self: Observable[A]) extends AnyVal {

    // Monix has a `.dump` transformation for this, but let's have a `.subscribe` alias
    def subscribePrintln(prefix: String = "") = self.subscribe { a =>
      println(prefix concat a.toString)
      Continue
    }

  }

  private val SOME_KEY = "FOO"

  def sleep(duration: Duration) = TimeUnit.MILLISECONDS.sleep(duration.toMillis)

  // 6
  Observable.unsafeCreate[String] { s =>
    s.onNext("Hello World!")
    s.onComplete()
    Cancelable.empty
  }.subscribePrintln()

  println("---------")

  {
    // 17
    val cache = new ConcurrentHashMap[String, String]
    cache.put(SOME_KEY, "123")
    Observable.unsafeCreate[String] { s =>
      s.onNext(cache.get(SOME_KEY))
      s.onComplete()
      Cancelable.empty
    }.subscribePrintln()
  }

  println("---------")

  {
    // 35
    def getFromCache(key: String) = s"$key:123"

    def getDataAsynchronously(key: String) =
      Future {
        sleep(1.second)
        s"$key:123"
      }

    def putInCache(key: String, value: String) = ()

    Observable.unsafeCreate[String] { s =>
      val fromCache = getFromCache(SOME_KEY)
      if (fromCache != null) {
        // emit synchronously
        s.onNext(fromCache)
        s.onComplete()
        Cancelable.empty
      } else {
        // fetch asynchronously
        getDataAsynchronously(SOME_KEY)
          .map { v =>
            putInCache(SOME_KEY, v)
            s.onNext(v)
            s.onComplete()
          }
          .failed.foreach { exception =>
          s.onError(exception)
        }
        Cancelable.empty
      }
    }.subscribePrintln()

    sleep(2.seconds)
  }

  println("---------")

  {
    // 81
    val o = Observable.unsafeCreate[Int] { s =>
      s.onNext(1)
      s.onNext(2)
      s.onNext(3)
      s.onComplete()
      Cancelable.empty
    }

    o.map(i => s"Number $i").subscribePrintln()
  }

  println("---------")

  {
    // 94
    Observable.unsafeCreate[Int] { s =>
      //... async subscription and data emission ...
      // it's generally advised to not start a thread like that inside an Observable
      new Thread(() => s.onNext(42), "MyThread").start()
      Cancelable.empty
    }
      .doOnNext(_ => println(Thread.currentThread()))
      .filter(_ % 2 == 0)
      .map(i => s"Value $i processed on ${Thread.currentThread()}")
      .subscribePrintln("SOME VALUE => ")
    println("Will print BEFORE values are emitted because Observable is async")
    sleep(1.second)
  }

  println("---------")

  // bits on legal/illegal skipped

  {
    // 142
    val a = Observable.unsafeCreate[String] { s =>
      new Thread { () =>
        s.onNext("one")
        s.onNext("two")
        s.onComplete()
      }.start()
      Cancelable.empty
    }
    val b = Observable.unsafeCreate[String] { s =>
      new Thread { () =>
        s.onNext("three")
        s.onNext("four")
        s.onComplete()
      }.start()
      Cancelable.empty
    }
    // this subscribes to a and b concurrently, and merges into a third sequential stream
    Observable.merge(a, b).subscribePrintln()

    sleep(10.millis)

  }

  println("---------")

  {
    // 164
    def getDataFromServerWithCallback(consumer: String => Unit) =   // got rid of useless `args`
      consumer(s"Random: ${Math.random}")

    val someData = Observable.unsafeCreate[String] { s =>
      getDataFromServerWithCallback { data =>
        s.onNext(data)
        s.onComplete()
      }
      Cancelable.empty
    }
    someData.subscribePrintln("Subscriber 1: ")
    someData.subscribePrintln("Subscriber 2: ")

    val lazyFallback = Observable("Fallback")
    someData.onErrorFallbackTo(lazyFallback).subscribePrintln()
  }

  println("---------")

  {
    // 205
    def getDataFromNetworkAsynchronously = Observable.range(0, 100).map(_.toString)

    getDataFromNetworkAsynchronously
      .drop(10)
      .take(5)
      .map(_ concat "_transformed")
      .subscribePrintln()
  }

  println("---------")

  {
    // 240+254
    def getDataAsObservable(i: Int): Observable[String] = Observable.now("Done: " + i + "\n")

    val o1 = getDataAsObservable(1)
    val o2 = getDataAsObservable(2)
    val o3 = Observable.zipMap2(o1, o2) { case (s1, s2) => s1 concat s2 }
    o3.subscribePrintln()

    println("---")

    val o3_ = Observable.merge(o1, o2)
    o3_.subscribePrintln()
  }

  println("---------")

  { // 265
    def getDataA: Task[String] = Task.now("DataA")     // ditto for 277
    def getDataB: Task[String] = Task.now("DataB")     //

    // merge a & b into an Observable stream of 2 values

    val a_merge_b = Observable.merge(
      Observable.fromTask(getDataA).subscribeOn(Scheduler.io()),
      Observable.fromTask(getDataB).subscribeOn(Scheduler.io())
    )
    a_merge_b.subscribePrintln()
    sleep(50.millis)   // gotta sleep here so that JVM doesn't exit immediately and we get to see the result

  }

  // Monix doesn't have Completable, pretty sure you can just use `Task[Unit]`

}
