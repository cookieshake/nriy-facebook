package net.ingtra.nriyfacebook.algolcheck

import com.mashape.unirest.http.Unirest
import org.json.JSONObject

import scala.collection.mutable

class AlgolData(json: JSONObject) {
  private val que = new mutable.Queue[JSONObject]
  private var currentJson = json
  que ++= getData()

  private def getData() = {
    val data = currentJson.getJSONArray("data")
    for (i <- 0 until data.length)
      yield data.get(i).asInstanceOf[JSONObject]
  }

  private def getNextPage(): JSONObject = Unirest.get(currentJson.getJSONObject("paging").getString("next")).asJson().getBody.getObject

  def hasNext(): Boolean = {
    if (que.length > 0) true
    else {
      if (currentJson.has("paging") && currentJson.getJSONObject("paging").has("next")) {
        currentJson = getNextPage()
        que ++= getData()
        hasNext
      } else false
    }
  }

  def next: JSONObject = {
    if (hasNext()) que.dequeue()
    else null
  }

  def foreach[U](f: (JSONObject) => U) = while (hasNext()) f(next)

}
