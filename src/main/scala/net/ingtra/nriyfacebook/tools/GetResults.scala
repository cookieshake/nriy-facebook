package net.ingtra.nriyfacebook.tools

import java.util.concurrent.TimeUnit

import org.mongodb.scala.Observable

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by ic on 2016-07-11.
  */
object GetResults {
  def apply[C](observable: Observable[C]): Seq[C] = Await.result(observable.toFuture(), Duration(10, TimeUnit.SECONDS))
}
