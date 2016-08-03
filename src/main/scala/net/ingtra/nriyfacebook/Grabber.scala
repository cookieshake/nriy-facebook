package net.ingtra.nriyfacebook


import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

import com.twitter.penguin.korean.TwitterKoreanProcessor
import net.ingtra.nriyfacebook.algolcheck.AlgolCheck
import net.ingtra.nriyfacebook.tools.{GetResults, TfMap}
import org.json.JSONObject
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonString}
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

    GetResults(collection.createIndex(Document({"{created_time:-1}"})))

    val algolCheck = new AlgolCheck(Setting.graphApiKey)
    val pageId = algolCheck.request(pageName).getString("id")

    val recentTime = GetResults(collection.find()
      .filter(Document(s"{_id:/^$pageId/}"))
      .projection(Document("{created_time:1}"))
      .sort(Document("{created_time:-1}")))
      .head.toBsonDocument
      .getString("created_time").getValue

    def putItToDb(json: JSONObject): Unit = {

      count += 1
      if (count % 1000 == 0) println(s"Grabbing $pageName : $count")

      json.put("_id", json.getString("id"))
      json.remove("id")

      try GetResults(collection.insertOne(Document(json.toString)))
      catch {
        case e: MongoWriteException => println(s"Count: $count " + e.getMessage)
      }


    }

    var switch = true
    val feeds = algolCheck.requestData(pageName + "/feed")
    while (switch && feeds.hasNext) {
      val feed = feeds.next
      if (feed.getString("created_time") <= recentTime) switch = false
      else putItToDb(feed)
    }

    count
  }

  def grabComment(): Unit = {
    val algolCheck = new AlgolCheck(Setting.graphApiKey)
    var count = 0

    val pageCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
      .getDatabase(Setting.pageGrabDbName)
      .getCollection(Setting.pageGrabCollName)
    val commentsCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
      .getDatabase(Setting.commentGrabDbName)
      .getCollection(Setting.commentGrabCollName)

    val ids = for (a <- GetResults(pageCollection.find().projection(BsonDocument("{_id:1}")))) yield a.toBsonDocument.getString("_id").getValue


    for (id <- ids) {
      count += 1
      if (count % 500 == 0) println(s"Count : $count")

      val bson = BsonDocument()
      val bsonArray = BsonArray()
      bson.put("_id", BsonString(id))

      val comments = algolCheck.requestData(id + "/comments", Seq("message_tags", "message", "created_time"))
      while (comments.hasNext) {
        val comment = comments.next
        bsonArray.add(BsonDocument(comment.toString))
      }

      bson.put("comments", bsonArray)

      try GetResults(commentsCollection.insertOne(Document(bson)))
      catch {
        case e: MongoWriteException => println(e.getMessage)
      }
    }
  }
}
