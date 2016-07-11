package net.ingtra.nriyfacebook.mongo

import java.util.concurrent.TimeUnit

import com.mongodb.client.model.{BulkWriteOptions, InsertManyOptions}
import org.mongodb.scala._
import org.mongodb.scala.bson.conversions

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class MongoDbCollection(host: String, dbName: String, collName: String) {
  val mongoClient: MongoClient = MongoClient("mongodb://" + host)
  val db: MongoDatabase = mongoClient.getDatabase(dbName)
  val coll: MongoCollection[Document] = db.getCollection(collName)

  implicit class DocumentObservable[C](val observable: Observable[Document]) extends  ImplicitObservable[Document] {
    override val converter: (Document) => String =  (doc) => doc.toJson
  }

  implicit class GenericObservable[C](val observable: Observable[C]) extends  ImplicitObservable[C] {
    override val converter: (C) => String = (doc) => doc.toString
  }

  trait ImplicitObservable[C] {
    val observable: Observable[C]
    val converter: (C) => String

    def results(): Seq[C] = Await.result(observable.toFuture(), Duration(10, TimeUnit.SECONDS))
    def headResult()=  Await.result(observable.head(), Duration(10, TimeUnit.SECONDS))
    def printResults(initial: String = ""): Unit = {
      if (initial.length > 0) print(initial)
      results().foreach(res => println(converter(res)))
    }
    def printHeadResult(initial: String = ""): Unit = println(s"${initial}${converter(headResult())}")
  }

  def exists(): Boolean = {
    if (!mongoClient.listDatabaseNames().results().contains(dbName)) return false
    if (!db.listCollectionNames().results().contains(collName)) return false
    true
  }

  def make(): Unit = {
    if (!mongoClient.listDatabaseNames().results().contains(dbName)) throw new Exception("No database in MongoDB: " + dbName)
    if (!db.listCollectionNames().results().contains(collName)) db.createCollection(collName)
  }

  def insertMany(seq: Seq[Document]): Unit = coll.insertMany(seq).results()

  def insertOne(doc: Document): Unit = coll.insertOne(doc).results()

  def createIndex(key: conversions.Bson): Unit = coll.createIndex(key).results()

  def find(filter: conversions.Bson): Seq[Document] = coll.find(filter).results()

  def drop(): Unit = coll.drop()

}
