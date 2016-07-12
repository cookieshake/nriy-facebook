package net.ingtra.nriyfacebook.tools

import java.util.concurrent.TimeUnit

import org.mongodb.scala.Observable

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object GetResults {
  def apply[C](observable: Observable[C]): Seq[C] = Await.result(observable.toFuture(), Duration(100, TimeUnit.SECONDS))
}
