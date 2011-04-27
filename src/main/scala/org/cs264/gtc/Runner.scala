package org.cs264.gtc

import org.joda.time.LocalDate
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet

object Runner {
  def main(args: Array[String]) = {
    getHotSearches((new LocalDate).minusDays(1))
  }

  private def getHotSearches(date: LocalDate): List[String] = {
    val client: HttpClient = new DefaultHttpClient
    val url = "http://www.google.com/trends/hottrends?sa=X&date=" + date
    val get = new HttpGet(url)
    val response = client.execute(get)
    val entity = response.getEntity
    if (entity != null) {
      val inputStream = entity.getContent
      try {
          for (line <- io.Source.fromInputStream(inputStream).getLines.filter(_.contains("class=num"))) {
            println(line)
          }
      } finally {
        inputStream.close
      }
    } else {
      //TODO cover else case
    }
    List("1")
  }
}