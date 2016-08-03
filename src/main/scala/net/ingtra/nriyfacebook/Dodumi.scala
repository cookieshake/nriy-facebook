package net.ingtra.nriyfacebook

import java.util.concurrent.{CopyOnWriteArrayList, LinkedBlockingDeque}
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference, DoubleAdder}

import net.ingtra.nriyfacebook.algolcheck.AlgolCheck
import net.ingtra.nriyfacebook.tools.{CosineSimularity, GetResults, TfMap}
import org.json.JSONObject
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{Document, MongoClient}
import org.mongodb.scala.bson.{BsonArray, BsonValue}

import scala.collection.mutable
import scala.util.Random
import scala.util.matching.Regex

object Dodumi {
  val idfedCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
    .getDatabase(Setting.idfedDbName)
    .getCollection(Setting.idfedCollName)

  private def bsonArrayToMap(bsonArray: BsonArray): Map[String, Double] = {
    val map = mutable.Map[String, Double]()

    for (i <- 0 until bsonArray.size()) {
      val bson = bsonArray.get(i).asDocument
      val str = bson.getString("string").getValue
      val tf = bson.getDouble("tf").getValue

      map(str) = tf
    }

    map.toMap
  }

  private def selectComment(comment: JSONObject): Boolean = {
    if (comment.has("message_tags")) return false

    val message = comment.getString("message")

    if (new Regex("#.*\\s").findFirstIn(message).nonEmpty) return false
    if (new Regex("@.*\\s").findFirstIn(message).nonEmpty) return false
    if (new Regex("\\(.*\\)").findFirstIn(message).nonEmpty) return false
    if (message.length < 3) return false
    if (20 < message.length) return false

    true
  }

  private def grabComments(id: String): Seq[String] = {
    val algolCheck = new AlgolCheck(Setting.graphApiKey)
    val comments = algolCheck.requestData(id + "/comments", Seq("message_tags", "message", "created_time"))
    val toReturn = mutable.ArrayBuffer[String]()

    for (cmt <- comments) {
      if (selectComment(cmt))
        toReturn.append(cmt.getString("message"))
    }

    toReturn
  }

  private def compare(string: String, thread: Int = 1): Array[(String, Double)] = {
    val tfMap = TfMap.withIdf(string, IdfTool.getIdf)
    val count = new AtomicInteger()
    val que = new LinkedBlockingDeque[Document]()

    val result = new CopyOnWriteArrayList[(String, Double)]()

    def handleDocument(doc: Document): Unit = {
      val bsonDoc = doc.toBsonDocument
      val bsonArray = bsonDoc.getArray("tokens")
      val tfMap2 = bsonArrayToMap(bsonArray)

      val id = bsonDoc.getString("_id").getValue
      val score = CosineSimularity(tfMap, tfMap2)

      val now = count.getAndAdd(1)
      if (now % 5000 == 0) println(s"Now: $now")

      result.add((id, score))
    }

    class DodumiWorker extends Thread {
      var flag = true

      override def run() = {
        while (!que.isEmpty || flag) {
          if (!que.isEmpty) {
            handleDocument(que.take())

          } else Thread.sleep(100)
        }
      }


      def exit() = flag = false
    }

    if (thread != 1) {
      val threads = for (i <- 1 to thread) yield new DodumiWorker()

      threads.foreach(_.start())

      var finished = false
      idfedCollection.find().subscribe((doc: Document) => que.put(doc), (err: Throwable) => println(err), () => finished = true)
      while (!finished) Thread.sleep(1000)

      threads.foreach(_.exit())
      threads.foreach(_.join())

    } else {
      var finished = false
      idfedCollection.find().subscribe((doc: Document) => handleDocument(doc), (err: Throwable) => println(err), () => finished = true)
      while (!finished) Thread.sleep(1000)
    }

    println(s"Finished : $count")

    var resultArray = result.toArray[(String, Double)](Array[(String, Double)]())

    resultArray = resultArray.sortBy(_._2)
    resultArray.reverse
  }

  def reply(message: String): String = {
    val scoreArray = compare(message)
    var comments = mutable.ArrayBuffer[(String, Double)]()

    var index = 0
    while (!(scoreArray(index)._2 < 0.3 || comments.length > 50) || (index < 5 || comments.length < 10)) {

      grabComments(scoreArray(index)._1).foreach(
        (cmt: String) => comments.append((cmt, CosineSimularity(TfMap.withIdf(message, IdfTool.getIdf), TfMap.withIdf(cmt, IdfTool.getIdf))))
      )

      index += 1
    }

    comments = comments.sortBy(_._2).reverse

    val high = comments.filter(_._2 > 0.4)

    if (high.nonEmpty) high(Random.nextInt(high.length))._1
    else comments(0)._1
  }

}
