package org.cs264.gtc

import org.joda.time.LocalDate
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import collection.mutable.ListBuffer

object Runner {
  def main(args: Array[String]) = {
    println(getHotSearches(System.getProperty("number.of.days").toInt))
  }

  private def getHotSearches(numOfDays: Int): IndexedSeq[String] = {
    val today = new LocalDate
    val hotSearches = for (day <- numOfDays to 1 by -1) yield getHotSearches(today.minusDays(day))
    hotSearches.flatten
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
            val hotSearch = line.substring(line.indexOfSlice("\">") + 2, line.indexOfSlice("</a>"))
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
}