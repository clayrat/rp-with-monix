package examples
package ch1

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._
import scala.concurrent.Future

import monix.eval.Task
import monix.execution.{Cancelable, Scheduler}
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Observable, OverflowStrategy}

import util.observable._
import util.time._

object Chapter1 extends App {

  private val SOME_KEY = "FOO"

  // gonna copy this numbering scheme, but it's not tied to pages or anything

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

    // replaced the mutable callback thingamajig with a Future
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
    // we can't use `unsafeCreate` to push multiple items because of backpressure concerns
    // see https://monix.io/docs/2x/reactive/observers.html#feeding-an-observer
    val o = Observable.create[Int](OverflowStrategy.DropOld(10)) { s =>
      s.onNext(1)
      s.onNext(2)
      s.onNext(3)
      s.onComplete()
      Cancelable.empty
    }

    o.map(i => s"Number $i").subscribePrintln()
    sleep(1.second)  // gotta sprinkle these `sleep`s so asynchrony doesn't mess up the output
  }

  println("---------")

  {
    // 94
    Observable.unsafeCreate[Int] { s =>
      // this is just for illustration, don't start threads inside Observable
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
    // this actually swallows "one" and "three", so again, don't do this
    val a = Observable.create[String](OverflowStrategy.DropOld(10)) { s =>
      new Thread { () =>
        s.onNext("one")
        s.onNext("two")
        s.onComplete()
      }.start()
      Cancelable.empty
    }
    val b = Observable.create[String](OverflowStrategy.DropOld(10)) { s =>
      new Thread { () =>
        s.onNext("three")
        s.onNext("four")
        s.onComplete()
      }.start()
      Cancelable.empty
    }
    // this subscribes to a and b concurrently, and merges into a third sequential stream
    Observable.merge(a, b).subscribePrintln()

    sleep(1.second)

  }

  println("---------")

  {
    // 164
    def getDataFromServerWithCallback(consumer: String => Unit): Unit =   // got rid of useless `args`
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

    sleep(1.second)

  }

  println("---------")

  { // 265
    // Monix has `Task` instead of `Single`
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

  // Monix doesn't have `Completable`, pretty sure you can just use `Task[Unit]`

}
