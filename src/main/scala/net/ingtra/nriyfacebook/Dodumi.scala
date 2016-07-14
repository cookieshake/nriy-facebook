package net.ingtra.nriyfacebook

import net.ingtra.nriyfacebook.tools.Namer
import org.mongodb.scala.bson.{BsonArray, BsonValue}

import scala.collection.mutable

object Dodumi {
  private def bsonArrayToMap(bsonArray: BsonArray): Map[String, Double] = {
    val map = mutable.Map[String, Double]()

    for (i <- 0 until bsonArray.size()) {
      val bson = bsonArray.get(i).asDocument
      val str = bson.getString(Namer.expand("string")).getValue
      val tf = bson.getDouble("tf").getValue

      map(str) = tf
    }

    map.toMap
  }
}
