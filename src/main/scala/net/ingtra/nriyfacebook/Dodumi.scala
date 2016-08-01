package net.ingtra.nriyfacebook

import java.util.concurrent.{CopyOnWriteArrayList, LinkedBlockingDeque}
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference, DoubleAdder}

import net.ingtra.nriyfacebook.tools.{CosineSimularity, GetResults, Namer, TfMap}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{Document, MongoClient}
import org.mongodb.scala.bson.{BsonArray, BsonValue}

import scala.collection.mutable

object Dodumi {
  val idfedCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
    .getDatabase(Setting.idfedDbName)
    .getCollection(Setting.idfedCollName)

  private def bsonArrayToMap(bsonArray: BsonArray): Map[String, Double] = {
    val map = mutable.Map[String, Double]()

    for (i <- 0 until bsonArray.size()) {
      val bson = bsonArray.get(i).asDocument
      val str = bson.getString(Namer.abbreviate("string")).getValue
      val tf = bson.getDouble("tf").getValue

      map(str) = tf
    }

    map.toMap
  }

  def compare(string: String, thread: Int = 1): Array[(String, Double)] = {
    val tfMap = TfMap(string)
    val count = new AtomicInteger()
    val que = new LinkedBlockingDeque[Document]()

    val result = new CopyOnWriteArrayList[(String, Double)]()

    def handleDocument(doc: Document): Unit = {
      val bsonDoc = doc.toBsonDocument
      val bsonArray = bsonDoc.getArray(Namer.abbreviate("tokens"))
      val tfMap2 = bsonArrayToMap(bsonArray)

      val id = bsonDoc.getString(Namer.abbreviate("id")).getValue
      val score = CosineSimularity(tfMap, tfMap2)

      val now = count.getAndAdd(1)
      if (now % 1000 == 0) println(s"Now: $now")

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

    val resultArray = result.toArray[(String, Double)](Array[(String, Double)]())
    resultArray.sortBy(_._2)
  }

}
