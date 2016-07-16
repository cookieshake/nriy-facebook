package net.ingtra.nriyfacebook

import net.ingtra.nriyfacebook.tools.{GetResults, Namer}
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.{BsonDocument, BsonDouble, BsonString}

import scala.collection.mutable

object IdfCalculator {
  val tokenizedCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
    .getDatabase(Setting.tokenizedDbName)
    .getCollection(Setting.tokenizedCollName)
  val idfCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
    .getDatabase(Setting.idfDbName)
    .getCollection(Setting.idfCollName)

  def putIdf(string: String, idf: Double): Unit = {
    val bson = BsonDocument()
    bson.put("_id", BsonString(string))
    bson.put("idf", BsonDouble(idf))

    idfCollection.insertOne(Document(bson))
  }

  def calculate(): Unit = {
    val tokenSet = mutable.HashSet[String]()

    for (content <- GetResults(tokenizedCollection.find())) {
      val tokens = content.toBsonDocument.getArray(Namer.abbreviate("tokens")).iterator()
      while (tokens.hasNext) {
        val token = tokens.next().asDocument().getString(Namer.abbreviate("string")).getValue
        if (!tokenSet.contains(token)) {
          val query = BsonDocument()

        }
      }
    }

  }



}
