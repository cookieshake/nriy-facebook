package net.ingtra.nriyfacebook


import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

import com.twitter.penguin.korean.TwitterKoreanProcessor
import net.ingtra.nriyfacebook.algolcheck.AlgolCheck
import net.ingtra.nriyfacebook.tools.{GetResults, TfMap}
import org.json.JSONObject
import org.mongodb.scala.bson.{BsonDocument, BsonString}
import org.mongodb.scala.{MongoClient, MongoCollection, MongoWriteException}
import org.mongodb.scala.bson.collection.immutable.Document

import scala.collection.mutable
import scala.util.matching.Regex


object Grabber {
  def grabPage(pageName: String): Int = {
    var count = 0
    val collection = MongoClient("mongodb://" + Setting.mongoDbHost)
      .getDatabase(Setting.pageGrabDbName)
      .getCollection(Setting.pageGrabCollName)

    val algolCheck = new AlgolCheck(Setting.graphApiKey)

    def putItToDb(json: JSONObject): Unit = {
      try GetResults(collection.insertOne(Document(json.toString)))
      catch { case e: MongoWriteException => println(s"Count: $count " + e.getMessage) }

      count += 1
      if (count % 1000 == 0) println(s"Grabbing $pageName : $count")
    }

    algolCheck.requestData(pageName + "/feed").foreach(putItToDb)
    count
  }

  def grabComment(id: String): Unit = {
    val algolCheck = new AlgolCheck(Setting.graphApiKey)
    val data = algolCheck.requestData(id + "/comments", Seq("message_tags", "message", "created_time"))

    while(data.hasNext) {
      val datum = data.next

      if (!datum.has("message_tags")) {
        var message = datum.getString("message")
        message = new Regex("\\(.*\\)").replaceAllIn(message, "")
        message = new Regex("#.*\\s").replaceAllIn(message, "")
        message = new Regex("@.*\\s").replaceAllIn(message, "")
        if (message.length != 3)
          println(message.trim)
      }

    }

  }
}

