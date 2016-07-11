package net.ingtra.nriyfacebook.algolcheck

import com.mashape.unirest.http.Unirest
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.json.JSONObject



class AlgolException(msg: String) extends Exception(msg) {

}

class AlgolCheck(token: String, version: String = "v2.6") {
  private val graphURL = "https://graph.facebook.com/" + version + "/"

  def request(rq: String = "me", fields: Seq[String] = Seq()): JSONObject = {
    val result = Unirest.get(graphURL + rq)
      .queryString("access_token", token)
      .queryString("fields", fields.mkString(","))
      .asJson.getBody.getObject

    if (result.has("error")) {
      val error = result.getJSONObject("error")
      throw new AlgolException(error.getString("type") + "--" + error.getString("message")) }
    else
      result
  }

  def requestData(rq: String = "me/feed", fields: Seq[String] = Seq()): AlgolData = new AlgolData(request(rq, fields))

  def getUserAccessToken(app_id: String, redirect_uri: String, app_secret: String, code: String): String = {
    Unirest.get(graphURL + "oauth/access_token")
      .queryString("client_id", app_id)
      .queryString("redirect_uri", redirect_uri)
      .queryString("client_secret", app_secret)
      .queryString("code", code)
      .asString().getBody
  }

}
