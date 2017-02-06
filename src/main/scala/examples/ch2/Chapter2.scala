package examples
package ch2

import scala.concurrent.duration._

import monix.execution.Ack.{Continue, Stop}
import monix.execution.Cancelable
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Observable, Observer}

import util.observable._
import util.time._

class Tweet(text: String) {
  def getText: String = text
}

object Chapter2 extends App {

  val tweets = Observable.empty //...

  {
    // 6
    tweets.subscribePrintln()
  }

  {
    // 17
    tweets.subscribe(
      { tweet =>
        println(tweet)
        Continue
      },
      _.printStackTrace()
    )
  }

  def noMore() = ()

  {
    // 27+38
    tweets.subscribe(
      { tweet =>
        println(tweet)
        Continue
      },
      _.printStackTrace(),
      noMore
    )
  }

  {
    // 51
    val observer = new Observer[Tweet] {
      def onNext(tweet: Tweet) = {
        println(tweet)
        Continue
      }

      def onError(e: Throwable) = e.printStackTrace()

      def onComplete() = noMore()
    }
    tweets.subscribe(observer)
  }

  {
    // 78
    val subscription = tweets.subscribePrintln()
    subscription.cancel()
  }

  {
    // 91
    val subscriber = new Observer[Tweet]() {
      def onNext(tweet: Tweet) =
      // `Subscription` in Monix is replaced with the `Ack` system
        if (tweet.getText.contains("Java")) Stop else Continue

      def onComplete() = ()

      def onError(e: Throwable) = e.printStackTrace()
    }
    tweets.subscribe(subscriber)
  }

  def log[A](msg: A) = println(Thread.currentThread.getName concat ": " concat msg.toString)

  {
    // 117
    log("Before")
    Observable.range(5, 8).subscribe { i => // range in Monix is [`from`, `to`) with optional `count`
      log(i)
      Continue
    }
    log("After")
  }

  println("---------")


  {
    // 135
    val ints = Observable.unsafeCreate[Int] { subscriber =>
      log("Create")
      subscriber.onNext(5)
      subscriber.onNext(6)
      subscriber.onNext(7)
      subscriber.onComplete()
      log("Completed")
      Cancelable.empty
    }

    log("Starting")
    ints.subscribe { i =>
      log("Element: " concat i.toString)
      Continue
    }
    log("Exit")
  }

  // `just` in Monix is `now`, defined @ monix.reactive.internal.builders.NowObservable

  println("---------")

  {
    // 162 + 177
    val ints = Observable.unsafeCreate[Int] { subscriber =>
      log("Create")
      subscriber.onNext(42)
      subscriber.onComplete()
      Cancelable.empty
    }
    log("Starting")
    ints.subscribe { i =>
      log("Element A: " concat i.toString)
      Continue
    }
    ints.subscribe { i =>
      log("Element B: " concat i.toString)
      Continue
    }
    log("Exit")

    println("   ---")

    val intsCached = ints.cache
    log("Starting")
    intsCached.subscribe { i =>
      log("Element A: " concat i.toString)
      Continue
    }
    intsCached.subscribe { i =>
      log("Element B: " concat i.toString)
      Continue
    }
    log("Exit")
  }

  println("---------")

  // skipped the endless loop

  {
    // 221
    val naturalNumbers = Observable.unsafeCreate[BigInt] { subscriber =>
      new Thread(() => {
        // again, don't do this manually
        var i = BigInt(0)
        while (subscriber.onNext(i) != Stop)
          i = i + 1
      }).start()
      // despite not defining `onComplete` below, this will work thanks to
      // the wrapper @ monix.reactive.internal.builders.UnsafeCreateObservable
      // should it be called `SortaSafe`? :)
      Cancelable(() => subscriber.onComplete())
    }

    val subscription = naturalNumbers.subscribe { i =>
      log(i.toString)
      Continue
    }
    sleep(1.millis) // this will still result in a lot of elements
    subscription.cancel

  }

  println("---------")

  def sleep_(duration: Duration) =
    try
      sleep(duration)
    catch {
      case _: InterruptedException =>
    }

  def delayed[T](x: T) = Observable.unsafeCreate[T] { subscriber =>
    new Thread(() => {
      sleep_(10.seconds)
      subscriber.onNext(x) // again, the check for completion is already made in the wrapper
    }).start()
    Cancelable(() => subscriber.onComplete())
  }

  def delayed2[T](x: T) = Observable.unsafeCreate[T] { subscriber =>
    val thread = new Thread(() => {
      sleep_(10.seconds)
      subscriber.onNext(x)
    })
    thread.start()
    Cancelable { () =>
      subscriber.onComplete() // signal completion first
      thread.interrupt()
    }
  }

  // skipped `loadAll`

  // no need to manually wrap in try-catch as in `rxLoad`, Monix does that for us

  // `fromCallable` in Monix is `eval`

  {
    // 304
    Observable.evalDelayed(1.second, 0L).subscribe { i =>
      log(i.toString)
      Continue
    }
    sleep(2.seconds)
  }

  println("---------")

  { // 311
    // you can see how initial 0 and subsequent values are emitted on different threads
    Observable.interval((1000000 / 60).microseconds).subscribe { i =>
      log(i.toString)
      Continue
    }
    sleep(2.seconds)
  }



}
