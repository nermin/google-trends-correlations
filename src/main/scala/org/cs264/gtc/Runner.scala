package org.cs264.gtc

import org.joda.time.LocalDate
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.{HttpPost, HttpGet}
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HTTP
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.util.EntityUtils
import collection.JavaConversions._
import collection.mutable.{ArrayBuffer, ListBuffer, Map}

object Runner {
  def main(args: Array[String]) = {
    //println(getHotSearches(System.getProperty("number.of.days").toInt).size)
    downloadCSV("ford","honda","mazda","toyota","nissan")
  }

  private def getHotSearches(numOfDays: Int): IndexedSeq[String] = {
    val today = new LocalDate
    val hotSearches = for (day <- numOfDays to 1 by -1) yield getHotSearches(today.minusDays(day))
    hotSearches.flatten.distinct
  }

  private def getHotSearches(date: LocalDate) = {
    val client: HttpClient = new DefaultHttpClient
    val url = "http://www.google.com/trends/hottrends?sa=X&date=" + date
    val get = new HttpGet(url)
    val response = client.execute(get)
    val entity = response.getEntity
    val hotSearches = new ListBuffer[String]

    if (entity != null) {
      val inputStream = entity.getContent
      try {
          for (line <- io.Source.fromInputStream(inputStream).getLines.filter(_.contains("class=num"))) {
            val hotSearch = line.substring(line.indexOfSlice("q=") + 2, line.indexOfSlice("&date"))
            hotSearches += hotSearch
          }
      } finally {
        inputStream.close
      }
    } else {
      //TODO cover else case
    }
    hotSearches.result
  }

  private def downloadCSV(searchTerms: String*) = {
    if (searchTerms.length > 5) throw new IllegalArgumentException("Google Trends accepts maximum of 5 search terms")
    val client: HttpClient = new DefaultHttpClient
    val post = new HttpPost("https://www.google.com/accounts/ClientLogin")
    val nvps = List[NameValuePair](new BasicNameValuePair("accountType", "GOOGLE"),
                                  new BasicNameValuePair("Email", "cs264.01@gmail.com"),
                                  new BasicNameValuePair("Passwd", "harvarduniv"),
                                  new BasicNameValuePair("service", "analytics"),
                                  new BasicNameValuePair("source", "cs264-finalproject"))
    post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8))
    val responsePOST = client.execute(post)
    val resEntity = responsePOST.getEntity
    var sid: String = ""
    if (resEntity != null) {
      sid = EntityUtils.toString(resEntity).split("\\r?\\n")(0)
    } else {
      //TODO cover else case
    }
    val httpGet = new HttpGet("http://www.google.com/trends/viz?q=" + searchTerms.mkString(",") + "&graph=all_csv&sa=N")
    httpGet.addHeader("Cookie", sid)
    val responseGET = client.execute(httpGet)

    val data = Map[String, ArrayBuffer[Float]]()
    for (searchTerm <- searchTerms) data += (searchTerm -> new ArrayBuffer[Float]())
    val entity = responseGET.getEntity

    if (entity != null) {
      val inputStream = entity.getContent
      try {
        val startsWithDate = """^([a-zA-Z]{3}\s\d{1,2}\s\d{4})""".r
        for (line <- io.Source.fromInputStream(inputStream).getLines if startsWithDate.findPrefixOf(line).isDefined) {
          println(line)
          val results = line.split(',').drop(1)
          var index = 0
          for (searchTerm <- searchTerms) {
            data.get(searchTerm).get += results(index).toFloat
            index += 2
          }
        }
        // last entry contains just zeros, so remove it
        for (searchTerm <- data)  searchTerm._2.remove(searchTerm._2.length - 1)
      } finally {
        inputStream.close
      }
    } else {
      //TODO cover else case
    }
    println("\n" + data)
  }
}