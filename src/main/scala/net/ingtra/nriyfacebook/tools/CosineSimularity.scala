package net.ingtra.nriyfacebook.tools

import scala.math._

//Python code from http://secmem.tistory.com/670

object CosineSimularity {
  def apply(map1: Map[Any, Double], map2: Map[Any, Double]): Double = {
    var maginitudeA: Double = 0
    map1.foreach((tuple: (Any, Double)) => maginitudeA += pow(tuple._2, 2))

    var maginitudeB: Double = 0
    map2.foreach((tuple: (Any, Double)) => maginitudeB += pow(tuple._2, 2))

    val maginitude: Double = sqrt(maginitudeA) + sqrt(maginitudeB)

    val unionKey = map1.keySet.filter(map2.contains)
    var docProduct: Double = 0
    unionKey.foreach((key: Any) => docProduct += map1(key) * map2(key))

    docProduct / maginitude
  }
}
