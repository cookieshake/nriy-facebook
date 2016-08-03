package net.ingtra.nriyfacebook

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

import net.ingtra.nriyfacebook.tools.{GetResults, TfMap}
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonDouble, BsonInt32, BsonString}
import org.mongodb.scala.bson.collection.immutable.Document

object Tokenizer {
  private def mapToBsonArray(map: Map[String, Double]): BsonArray = {
    val bsonArray = BsonArray()

    for (tuple <- map) {
      val bson = BsonDocument()
      bson.put("string", BsonString(tuple._1))
      bson.put("tf", BsonDouble(tuple._2))

      bsonArray.add(bson)
    }

    bsonArray
  }

  private def contentToDoc(content: Document): Document = {
    val bsonContent = content.toBsonDocument
    val id = bsonContent.getString("_id")
    val msg = bsonContent.getString("message")

    val bsonDoc = BsonDocument()
    bsonDoc.append("_id", id)
    bsonDoc.append("tokens",mapToBsonArray(TfMap(msg.getValue)))

    Document(bsonDoc)
  }

  def apply(threads: Int = 1): Unit = {
    val tokenizedCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
      .getDatabase(Setting.tokenizedDbName)
      .getCollection(Setting.tokenizedCollName)

    val indexQuery = BsonDocument()
    indexQuery.put("tokens" + "." + "string", BsonInt32(-1))
    GetResults(tokenizedCollection.createIndex(indexQuery))

    val pageCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
      .getDatabase(Setting.pageGrabDbName)
      .getCollection(Setting.pageGrabCollName)

    val que = new LinkedBlockingQueue[Document]()
    val count = new AtomicInteger(0)

    class TokenizeWorker extends Thread {
      var flag = true

      override def run() = {
        while (!que.isEmpty || flag) {
          if (!que.isEmpty) {
            try {
              val content = que.take()
              val doc = contentToDoc(content)
              GetResults(tokenizedCollection.insertOne(doc))
            } catch {
              case e: Exception => println(e.getMessage)
            }

            val now = count.addAndGet(1)
            if (now % 1000 == 0 && now != 0) println("Tokenizing: " + now)

          } else Thread.sleep(100)
        }
      }

      def exit() = flag = false
    }

    val threadSeq = for (i <- 1 to threads) yield new TokenizeWorker()
    threadSeq.foreach(_.start)

    var finished = false
    pageCollection.find().subscribe((doc: Document) => que.put(doc), (err: Throwable) => println(err), () => finished = true)
    while (!finished) Thread.sleep(1000)

    threadSeq.foreach(_.exit())
    threadSeq.foreach(_.join())
  }
}
