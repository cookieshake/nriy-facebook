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
      .append("_id", BsonString(string))
      .append("idf", BsonDouble(idf))

    try
      GetResults(idfCollection.insertOne(Document(bson)))
    catch {
      case e: Exception => println(e)
    }
  }

  def calculate(): Unit = {
    val tokenSet = mutable.HashSet[String]()
    val documentCount = GetResults(tokenizedCollection.count()).head
    var count = 0

    def handleContent(content: Document): Unit = {
      val tokens = content.toBsonDocument.getArray(Namer.abbreviate("tokens")).iterator()
      while (tokens.hasNext) {
        val token = tokens.next().asDocument().getString(Namer.abbreviate("string")).getValue
        if (!tokenSet.contains(token)) {
          val query = BsonDocument()
            .append(Namer.abbreviate("tokens"), BsonDocument()
              .append("$elemMatch", BsonDocument()
                .append(Namer.abbreviate("string"), BsonString(token))))
          val haveToken = GetResults(tokenizedCollection.count(query)).head
          val idf = math.log(documentCount / haveToken)

          putIdf(token, idf)
          tokenSet.add(token)
        }
      }
      count += 1
      if (count % 500 ==0) println(s"Calculating: $count/$documentCount")
    }

    var finished = false
    tokenizedCollection.find().subscribe((doc: Document) => handleContent(doc), (err: Throwable) => println(err), () => finished = true)

    while (!finished) Thread.sleep(1000)

  }



}
