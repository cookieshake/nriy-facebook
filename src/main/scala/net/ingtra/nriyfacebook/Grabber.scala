package net.ingtra.nriyfacebook

import net.ingtra.nriyfacebook.algolcheck.AlgolCheck
import net.ingtra.nriyfacebook.mongo.MongoDbCollection
import net.ingtra.nriyfacebook.tools.Namer
import org.json.JSONObject
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.bson.collection.immutable.Document


class Grabber() {
  def callCollection(): MongoDbCollection = {
    val collection = new MongoDbCollection(Setting.mongoDbHost, Setting.pageGrabDbName, Setting.pageGrabCollName)
    if (!collection.exists()) {
      collection.make()
    }
    collection
  }

  def grabPage(pageName: String): Int = {
    var count = 0
    val collection = callCollection()
    val algolCheck = new AlgolCheck(Setting.graphApiKey)

    def putItToQue(json: JSONObject): Unit = {
      val abbreviated = Namer.abbreviateJson(json)

      try collection.insertOne(Document(abbreviated.toString))
      catch { case e: MongoWriteException => println(e.getMessage) }

      count += 1
      if (count % 1000 == 0) println(s"Grabbing $pageName : $count")
    }

    algolCheck.requestData(pageName + "/feed").foreach(putItToQue)
    count
  }
}
