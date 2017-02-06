package examples
package ch2

import scala.concurrent.duration._

import monix.execution.Ack.{Continue, Stop}
import monix.execution.Cancelable
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

import org.log4s._
import twitter4j._

import examples.util.time._

object Twitter extends App {

  private[this] val log = getLogger

  {
    // 18
    val twitterStream = new TwitterStreamFactory().getInstance
    twitterStream.addListener(new StatusListener() {
      def onStatus(status: Status) =
        log.info(s"Status: $status")

      def onException(ex: Exception) =
        log.error(s"Error callback $ex")

      def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) = ()

      def onTrackLimitationNotice(numberOfLimitedStatuses: Int) = ()

      def onScrubGeo(userId: Long, upToStatusId: Long) = ()

      def onStallWarning(warning: StallWarning) = ()
    })
    twitterStream.sample()
    sleep(10.seconds)
    twitterStream.shutdown()
  }

  // skip `consume`

  {
    // 150
    def observe =
      Observable.unsafeCreate[Status] { subscriber =>
        val twitterStream = new TwitterStreamFactory().getInstance
        twitterStream.addListener(new StatusListener() {
          def onStatus(status: Status) =
            if (subscriber.onNext(status) == Stop)
              twitterStream.shutdown()

          def onException(ex: Exception) = {
            subscriber.onError(ex)
            twitterStream.shutdown()
          }

          def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) = ()

          def onTrackLimitationNotice(numberOfLimitedStatuses: Int) = ()

          def onScrubGeo(userId: Long, upToStatusId: Long) = ()

          def onStallWarning(warning: StallWarning) = ()
        })
        twitterStream.sample()
        Cancelable { () =>
          subscriber.onComplete()
          twitterStream.shutdown()
        }
      }

    val sub = observe.subscribe({ status =>
      log.info(s"Status: $status")
      Continue
    },
      ex => log.error(s"Error callback $ex")
    )
    sleep(10.seconds)
    sub.cancel()

  }

}
