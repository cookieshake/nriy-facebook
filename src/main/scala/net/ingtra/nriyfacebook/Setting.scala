package net.ingtra.nriyfacebook

import java.io.FileNotFoundException

import org.json.{JSONException, JSONObject}

import scala.io.Source

object Setting {
  var setting: JSONObject = null

  var mongoDbHost: String = null
  var graphApiKey: String = null

  var pageGrabDbName: String = null
  var pageGrabCollName: String = null

  var tokenizedDbName: String = null
  var tokenizedCollName: String = null

  var idfDbName: String = null
  var idfCollName: String = null

  var termIdDbName: String = null
  var termIdCollName: String = null

  var idfedDbName: String = null
  var idfedCollName: String = null


  try {
    setting = new JSONObject(Source.fromFile("./setting.json").mkString)

    mongoDbHost = setting.getString("mongoDbHost")
    graphApiKey = setting.getString("graphApiKey")

    pageGrabDbName = setting.getString("pageGrabDbName")
    pageGrabCollName = setting.getString("pageGrabCollName")

    tokenizedDbName = setting.getString("tokenizedDbName")
    tokenizedCollName = setting.getString("tokenizedCollName")

    idfDbName = setting.getString("idfDbName")
    idfCollName = setting.getString("idfCollName")

    termIdDbName = setting.getString("termIdDbName")
    termIdCollName = setting.getString("termIdCollName")

    idfedDbName = setting.getString("idfedDbName")
    idfedCollName = setting.getString("idfedCollName")

  } catch {
    case e: FileNotFoundException => println("No setting.json!"); System.exit(0)
    case e: JSONException => println("Please check setting.json!"); System.exit(0)
  }
}
