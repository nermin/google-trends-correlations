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
import collection.mutable.{ListBuffer, HashMap}

object Runner {
  def main(args: Array[String]) = {

    val hotSearches = getHotSearches(System.getProperty("number.of.days").toInt)
    val data = new HashMap[String, List[Double]]
    for (hotSearch <- hotSearches) data += (hotSearch -> downloadCSV(hotSearch))

    for (outHS <- data) {
      for (inHS <- data) {
        calculateCorrelation((inHS._1, inHS._2),(outHS._1, outHS._2))
      }
    }
  }

  private def calculateCorrelation(termX: (String, List[Double]), termY: (String, List[Double])) = {
    val n = termX._2.length
    val sumX = termX._2.sum
    val sumY = termY._2.sum
    val sumXY = (termX._2, termY._2).zipped.map(_ * _).sum
    val sumXSquare = termX._2.map(e => e * e).sum
    val sumYSquare = termY._2.map(e => e * e).sum
    val r = ((n * sumXY) - (sumX * sumX)) /
      math.sqrt(((n * sumXSquare) - (sumX * sumX)) * ((n * sumYSquare) - (sumY * sumY)))
    println(r)
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

  private def downloadCSV(searchTerm: String) = {
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
    val httpGet = new HttpGet("http://www.google.com/trends/viz?q=" + searchTerm + "&graph=all_csv&sa=N")
    httpGet.addHeader("Cookie", sid)
    val responseGET = client.execute(httpGet)

    val data = new ListBuffer[Double]
    val entity = responseGET.getEntity

    if (entity != null) {
      val inputStream = entity.getContent
      try {
        val startsWithDate = """^([a-zA-Z]{3}\s\d{1,2}\s\d{4})""".r
        for (line <- io.Source.fromInputStream(inputStream).getLines if startsWithDate.findPrefixOf(line).isDefined) {
          data += line.split(',')(1).toDouble
        }
      } finally {
        inputStream.close
      }
    } else {
      //TODO cover else case
    }
    data.result
  }
}