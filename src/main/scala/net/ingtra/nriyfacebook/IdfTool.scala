package net.ingtra.nriyfacebook

import net.ingtra.nriyfacebook.tools.{GetResults, Namer}
import org.bson.BsonValue
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonDouble, BsonString, BsonValue}

import scala.collection.mutable

object IdfTool {
  val tokenizedCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
    .getDatabase(Setting.tokenizedDbName)
    .getCollection(Setting.tokenizedCollName)
  val idfCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
    .getDatabase(Setting.idfDbName)
    .getCollection(Setting.idfCollName)
  val idfedCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
    .getDatabase(Setting.idfedDbName)
    .getCollection(Setting.idfedCollName)


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
      val tokens = content.toBsonDocument.getArray("tokens").iterator()
      while (tokens.hasNext) {
        val token = tokens.next().asDocument().getString("string").getValue
        if (!tokenSet.contains(token)) {
          val query = BsonDocument()
            .append("tokens", BsonDocument()
              .append("$elemMatch", BsonDocument()
                .append("string", BsonString(token))))
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

  def getIdf(string: String): Double = {
    val query = BsonDocument().append("_id", BsonString(string))
    val findResult = GetResults(idfCollection.find(query).first)

    if (findResult.size != 0)
      findResult.head.toBsonDocument.getDouble("idf").getValue
    else
      1.0
  }//GetResults(idfCollection.find(BsonDocument().append("_id", BsonString(string))).first).head.toBsonDocument.getDouble("idf").getValue

  def transitTokenizeds(): Unit = {
    val idfMap = mutable.HashMap[String, Double]()
    var count = 0

    def handleDocument(doc: Document): Unit = {
      def idf(string: String): Double = {
        if (idfMap.contains(string)) idfMap(string)
        else {
          val idfDouble = getIdf(string)
          idfMap(string) = idfDouble
          idfDouble
        }
      }

      val bsonDoc = doc.toBsonDocument
      val bsonId = bsonDoc.getString("_id")
      val tokenArray = bsonDoc.getArray("tokens")
      val changedArray = BsonArray()


      for (i <- 0 until tokenArray.size) {
        val token = tokenArray.get(i).asDocument()
        val changedToken = BsonDocument()

        val bsonString = token.getString("string")
        val tf = token.getDouble("tf").getValue

        changedToken.put("string", bsonString)
        changedToken.put("tf", BsonDouble(tf * idf(bsonString.getValue)))

        changedArray.add(changedToken)
      }

      bsonDoc.replace("tokens", changedArray)

      GetResults(idfedCollection.insertOne(Document(bsonDoc)))
      count += 1
      if (count % 1000 == 0) println(s"Now: $count")
    }

    var finished = false
    tokenizedCollection.find().subscribe((doc: Document) => handleDocument(doc), (err: Throwable) => println(err), () => finished = true)
    while (!finished) Thread.sleep(100)
  }

}
