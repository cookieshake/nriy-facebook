package net.ingtra.nriyfacebook.tools


import com.twitter.penguin.korean.TwitterKoreanProcessor._

import scala.collection.mutable

object TfMap {
  def apply(str: String): Map[String, Double] = {
    val stopWords = Array("Space", "Punctuation", "Josa", "Eomi", "Hashtag", "Conjunction", "Number")
    val map = mutable.Map[String, Double]()
    val tokenized = stem(tokenize(str))

    for (token <- tokenized) {
      if (!stopWords.contains(token.pos.toString)) {
        if (!map.contains(token.text)) map(token.text) = 1
        else map(token.text) += 1
      }
    }

    map.foreach(
      (tuple: (String, Double)) => map(tuple._1) = tuple._2 / tokenized.size
    )

    map.toMap
  }

  def withIdf(str: String, idf: (String) => (Double)): Map[String, Double] = {
    val map = apply(str)
    val idfMap = mutable.Map[String, Double]()

    map.foreach(
      (tuple: (String, Double)) => idfMap(tuple._1) = tuple._2 * idf(tuple._1)
    )

    idfMap.toMap
  }
}
