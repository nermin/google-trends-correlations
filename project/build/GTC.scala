import sbt._
class GTC (info: ProjectInfo) extends DefaultProject(info) {
  System.setProperty("number.of.days", "3")
  override def mainClass = Some("org.cs264.gtc.Runner")
}