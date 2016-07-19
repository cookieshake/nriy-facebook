package net.ingtra.nriyfacebook.tools

object Timer {
  var startTime: Long = -1

  def start(): Unit = startTime = System.nanoTime()
  def end(): Unit = {
    if (startTime == -1) println("Not started!")
    else {
      val timeElapsed: Double = (System.nanoTime() - startTime).asInstanceOf[Double] / 1000000000
      println(s"Time Elapsed: $timeElapsed")
    }
  }
}
