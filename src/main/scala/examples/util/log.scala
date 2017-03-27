package examples.util

import java.time.LocalTime

object log {

  def log[A](msg: A) = println(Thread.currentThread.getName concat ": " concat msg.toString)

  def logTs[A](msg: A) = println(LocalTime.now.toString concat ": " concat msg.toString)

}
