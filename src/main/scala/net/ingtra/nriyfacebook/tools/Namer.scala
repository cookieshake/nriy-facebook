package net.ingtra.nriyfacebook.tools

import org.json.JSONObject
import org.mongodb.scala.Document

import scala.collection.mutable

object Namer {
  val map = Map(
    "created_time" -> "ct",
    "id" -> "_id",
    "message" -> "ms",
    "data" -> "dt",
    "message_tags" -> "mt",
    "type" -> "tp",
    "offset" -> "os",
    "length" -> "lt",
    "name" -> "nm",
    "cursors" -> "cs",
    "before" -> "bf",
    "after" -> "af",
    "next" -> "nx",
    "previous" -> "pv",
    "paging" -> "pg",
    "tokens" -> "ts",
    "string" -> "st"
  )

  val reversedMap = mutable.Map.empty[String, String]
  for (w <- map) reversedMap += w.swap

  def abbreviate(from: String): String = {
    if (map.contains(from)) map(from)
    else from
  }

  def expand(from: String): String = {
    if (reversedMap.contains(from)) reversedMap(from)
    else from
  }

  private def changeJsonKeys(from: JSONObject, how: (String) => (String)): JSONObject = {
    val json = from
    val keys = json.keySet().toArray(new Array[String](0))

    def change(key: String): Unit = {
      if (json.optJSONObject(key) != null) json.put(key, changeJsonKeys(json.getJSONObject(key), how))

      if (json.optJSONArray(key) != null) {
        val array = json.getJSONArray(key)
        for (i <- 0 until array.length())
          array.put(i, changeJsonKeys(array.getJSONObject(i), how))
      }

      if (key != how(key)) {
        json.put(how(key), json.get(key))
        json.remove(key)
      }
    }

    keys.foreach(change)
    json
  }

  def abbreviateJson(from: JSONObject): JSONObject = changeJsonKeys(from, abbreviate)
  def expandJson(from: JSONObject): JSONObject = changeJsonKeys(from, expand)

  def abbreviateDocument(from: Document): Document = Document(abbreviateJson(new JSONObject(from.toJson)).toString)
  def expandDocument(from: Document): Document = Document(expandJson(new JSONObject(from.toJson)).toString)
}