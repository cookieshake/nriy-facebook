package net.ingtra.nriyfacebook

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

import net.ingtra.nriyfacebook.tools.{GetResults, Namer, TfMap}
import org.json.JSONObject
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonDouble, BsonString}
import org.mongodb.scala.bson.collection.immutable.Document

/**
  * Created by ic on 2016-07-13.
  */
object Tokenizer {
  private def mapToBsonArray(map: Map[String, Double]): BsonArray = {
    val bsonArray = BsonArray()

    for (tuple <- map) {
      val bson = BsonDocument()
      bson.put(Namer.abbreviate("string"), BsonString(tuple._1))
      bson.put("tf", BsonDouble(tuple._2))

      bsonArray.add(bson)
    }

    bsonArray
  }

  private def contentToDoc(content: Document): Document = {
    val bsonContent = content.toBsonDocument
    val id = bsonContent.getString(Namer.abbreviate("id"))
    val msg = bsonContent.getString(Namer.abbreviate("message"))

    val bsonDoc = BsonDocument()
    bsonDoc.append("_id", id)
    bsonDoc.append(Namer.abbreviate("tokens"),mapToBsonArray(TfMap(msg.getValue)))

    Document(bsonDoc)
  }

  def apply(thr: Int = 1): Unit = {
    val tokenizedCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
      .getDatabase(Setting.tokenizedDbName)
      .getCollection(Setting.tokenizedCollName)
    val pageCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
      .getDatabase(Setting.pageGrabDbName)
      .getCollection(Setting.pageGrabCollName)

    val que = new LinkedBlockingQueue[Document]()
    val count = new AtomicInteger(0)

    class tokenizer extends Thread {
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

            if (count.get() % 1000 == 0) println("Tokenizing: " + count.get())
            count.addAndGet(1)
          }
        }
      }

      def exit() = flag = false
    }

    val threads = for (i <- 1 to thr) yield new tokenizer()
    threads.foreach(_.start)

    val docs = GetResults(pageCollection.find())
    docs.foreach(que.put)
    threads.foreach(_.exit())
  }
}
