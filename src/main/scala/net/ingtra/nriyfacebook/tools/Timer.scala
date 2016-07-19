package net.ingtra.nriyfacebook.tools

object Timer {
  var startTime: Long = null

  def start(): Unit = startTime = System.nanoTime()
  def end(): Unit = {
    if (startTime == null) println("Not started!")
    else {
      val timeElapsed: Double = (System.nanoTime() - startTime).asInstanceOf[Double] / 1000000000
      println(s"Time Elapsed: $timeElapsed")
    }
  }
}
