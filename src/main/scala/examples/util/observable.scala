package examples.util

import monix.execution.Ack.Continue
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

import log.logTs

object observable {

  implicit class PrinterObservable[A](val self: Observable[A]) extends AnyVal {

    // Monix has a `.dump` transformation for this, but let's have a `.subscribe` alias
    def subscribePrintln(prefix: String = "") = self.subscribe { a =>
      println(prefix concat a.toString)
      Continue
    }

    def subscribeLog() = self.subscribe { a =>
      logTs(a)
      Continue
    }

  }

}
