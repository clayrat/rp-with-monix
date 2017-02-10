package examples
package ch2

import scala.concurrent.duration._

import monix.execution.Ack.{Continue, Stop}
import monix.execution.Cancelable
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject

import org.log4s._
import twitter4j._

import examples.util.time._

object Twitter extends App {

  private[this] val log = getLogger

  def mkTwitterStream(onStatus: Status => Unit, onException: Exception => Unit) = {
    val ts = new TwitterStreamFactory().getInstance()
    ts.addListener(new StatusListener() {
      def onStatus(status: Status) = onStatus(status)
      def onException(ex: Exception) = onException(ex)

      def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) = ()
      def onTrackLimitationNotice(numberOfLimitedStatuses: Int) = ()
      def onScrubGeo(userId: Long, upToStatusId: Long) = ()
      def onStallWarning(warning: StallWarning) = ()
    })
    ts
  }

  {
    // 18
    val twitterStream = mkTwitterStream(
      status => log.info(s"Status: $status"),
      ex => log.error(s"Error callback $ex")
    )
    twitterStream.sample()
    sleep(10.seconds)
    twitterStream.shutdown()
  }

  // skipped `consume`

  {
    // 150
    def observe =
      Observable.unsafeCreate[Status] {
        subscriber =>
          val twitterStream = new TwitterStreamFactory().getInstance
          twitterStream.addListener(new StatusListener() {
            def onStatus(status: Status) =
              if (subscriber.onNext(status) == Stop)
                twitterStream.shutdown()

            def onException(ex: Exception) = {
              subscriber.onError(ex)
              twitterStream.shutdown() // UnsafeCreateObservable stops the stream in this case so we shutdown as well
            }

            def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) = ()
            def onTrackLimitationNotice(numberOfLimitedStatuses: Int) = ()
            def onScrubGeo(userId: Long, upToStatusId: Long) = ()
            def onStallWarning(warning: StallWarning) = ()
          })
          twitterStream.sample()
          Cancelable {
            () =>
              subscriber.onComplete()
              twitterStream.shutdown()
          }
      }

    val sub = observe.subscribe({
      status =>
        log.info(s"Status: $status")
        Continue
    },
      ex => log.error(s"Error callback $ex")
    )
    sleep(10.seconds)
    sub.cancel()

  }

  // skipped LazyTwitterObservable

  class TwitterSubject {
    //
    private val subject = PublishSubject[Status]()

    val twitterStream = mkTwitterStream(
      status => subject.onNext(status),
      ex => subject.onError(ex)
    )
    twitterStream.sample()

    def observe: Observable[Status] = subject
  }

  {
    // 186
    def status = Observable.unsafeCreate[Status] {
      subscriber =>
        println("Establishing connection")
        val twitterStream = mkTwitterStream(_ => (), _ => ())
        twitterStream.sample()
        Cancelable {
          () =>
            println("Disconnecting")
            subscriber.onComplete()
            twitterStream.shutdown()
        }
    }

    // `publish` in Monix uses `PublishSubject` internally, so this is actually very similar to the previous case
    val lzy = status.publish.refCount        // aka `share`
    println("Before subscribers")
    val sub1 = lzy.subscribe
    println("Subscribed 1")
    val sub2 = lzy.subscribe
    println("Subscribed 2")
    sub1.cancel()
    println("Unsubscribed 1")
    sub2.cancel()
    println("Unsubscribed 2")

    //val published = status.publish
    //published.connect
  }

}


