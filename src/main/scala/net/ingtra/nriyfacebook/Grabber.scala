package net.ingtra.nriyfacebook


import java.util.concurrent.LinkedBlockingQueue

import net.ingtra.nriyfacebook.algolcheck.AlgolCheck
import net.ingtra.nriyfacebook.tools.{GetResults, Namer, TfMap}
import org.json.JSONObject
import org.mongodb.scala.{MongoClient, MongoCollection, MongoWriteException}
import org.mongodb.scala.bson.collection.immutable.Document


object Grabber {
  private def callPageCollection(): MongoCollection[Document] = {
    val db = MongoClient("mongodb://" + Setting.mongoDbHost).getDatabase(Setting.pageGrabDbName)
    val coll = db.getCollection(Setting.pageGrabCollName)

    if (!GetResults(db.listCollectionNames()).contains(Setting.pageGrabCollName))
      GetResults(db.createCollection(Setting.pageGrabCollName))


    coll
  }

  def grabPage(pageName: String): Int = {
    var count = 0
    val collection = callPageCollection()
    val algolCheck = new AlgolCheck(Setting.graphApiKey)

    def putItToDb(json: JSONObject): Unit = {
      val abbreviated = Namer.abbreviateJson(json)

      try GetResults(collection.insertOne(Document(abbreviated.toString)))
      catch { case e: MongoWriteException => println(s"Count: $count " + e.getMessage) }

      count += 1
      if (count % 1000 == 0) println(s"Grabbing $pageName : $count")
    }

    algolCheck.requestData(pageName + "/feed").foreach(putItToDb)
    count
  }

  def tokenize(): Unit = {
    val tokenizedCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
      .getDatabase(Setting.tokenizedDbName)
      .getCollection(Setting.tokenizedCollName)

    val pageCollection = callPageCollection()

    val que = new LinkedBlockingQueue[Document]()

    val thread = new Thread {
      override def run() = {
        while (true) {
          if(!que.isEmpty) {
            val json = new JSONObject(que.peek().toJson)
            val msg = json.getString(Namer.abbreviate("message"))

            println(TfMap(msg))
          }
        }
      }
    }

    thread.start()

    val docs = GetResults(pageCollection.find())
    que.put(docs.head)

  }
}
