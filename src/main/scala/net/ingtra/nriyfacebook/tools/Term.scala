package net.ingtra.nriyfacebook.tools

import net.ingtra.nriyfacebook.Setting
import org.mongodb.scala.MongoClient

/**
  * Created by ic on 2016-07-12.
  */
object Term {
  val idfCollection = MongoClient("mongodb://" + Setting.mongoDbHost)
    .getDatabase(Setting.idfDbName)
    .getCollection(Setting.idfCollName)

  val idfMap = Map[String, Double]()
}
