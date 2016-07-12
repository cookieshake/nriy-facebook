package net.ingtra.nriyfacebook

import org.json.JSONObject

import scala.io.Source

object Setting {
  private val setting = new JSONObject(Source.fromFile("./setting.json").mkString)

  val mongoDbHost = setting.getString("mongoDbHost")
  val graphApiKey = setting.getString("graphApiKey")

  val pageGrabDbName = setting.getString("pageGrabDbName")
  val pageGrabCollName = setting.getString("pageGrabCollName")

  val tokenizedDbName = setting.getString("tokenizedDbName")
  val tokenizedCollName = setting.getString("tokenizedCollName")

  val idfDbName = setting.getString("idfDbName")
  val idfCollName = setting.getString("idfCollName")
}
