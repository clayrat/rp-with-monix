package examples.util

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

object time {

  def sleep(duration: Duration) =
    TimeUnit.MILLISECONDS.sleep(duration.toMillis)

}
