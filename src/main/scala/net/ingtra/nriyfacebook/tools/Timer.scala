package net.ingtra.nriyfacebook.tools

object Timer {
  var startTime: Long = 0

  def start(): Unit = startTime = System.nanoTime()
  def end(): Unit = {
    val timeElapsed: Double = (System.nanoTime() - startTime).asInstanceOf[Double] / 1000000000
    println(s"Time Elapsed: $timeElapsed")
  }
}
