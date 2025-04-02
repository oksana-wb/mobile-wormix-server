package wormix

import scala.collection.mutable
import scala.io.Source

object FindColliseumCheaters extends App {
  val levelsMap = mutable.Map[String, Integer]()
  val result = mutable.Map[String, Integer]()

  val wipes = Source.fromFile(s"logs/cdr/unpacked_ok/result.csv").getLines().map("2:" + _).toSet

  Array(
    "pvp_stat-2015-10-01.log",
    "pvp_stat-2015-10-02.log",
    "pvp_stat-2015-10-03.log",
    "pvp_stat-2015-10-04.log",
    "pvp_stat-2015-10-05.log",
    "pvp_stat-2015-10-06.log",
    "pvp_stat-2015-10-07.log",
    "pvp_stat-2015-10-08.log",
    "pvp_stat-2015-10-09.log",
    "pvp_stat-2015-10-10.log",
    "pvp_stat-2015-10-11.log",
    "pvp_stat-2015-10-12.log",
    "pvp_stat-2015-10-13.log",
    "pvp_stat-2015-10-14.log",
    "pvp_stat-2015-10-15.log",
    "pvp_stat-2015-10-16.log",
    "pvp_stat-2015-10-17.log",
    "pvp_stat-2015-10-18.log",
    "pvp_stat-2015-10-19.log",
    "pvp_stat-2015-10-20.log",
    "pvp_stat-2015-10-21.log",
    "pvp_stat-2015-10-22.log",
    "pvp_stat-2015-10-23.log",
    "pvp_stat-2015-10-24.log",
    "pvp_stat-2015-10-25.log",
    "pvp_stat-2015-10-26.log",
    "pvp_stat-2015-10-27.log",
    "pvp_stat-2015-10-28.log",
    "pvp_stat-2015-10-29.log",
    "pvp_stat-2015-10-30.log",
    "pvp_stat-2015-10-31.log",
    "pvp_stat-2015-11-01.log",
    "pvp_stat-2015-11-02.log"
  ).reverse.foreach(parseLines)

  def parseLines(file: String): Unit = {
    println(file + " ...")
    for(arr <- Source.fromFile(s"logs/cdr/unpacked_ok/$file").getLines().toList.reverse
      .map(arr => "" +: arr.split("\t"))
    ) {
      if(arr(3).toInt == 4 && arr(4).startsWith("null/")) {
        sub(arr(7))
        sub(arr(19))

        def sub(uid: String): Unit = {
          val level = levelsMap.getOrElse[Integer](uid, 0)
          if(level > 0 && level < 11 && !wipes.contains(uid)) {
            val key = s"${uid} [$level]"
            val count = result.getOrElse[Integer](key, 0)
            result(key) = count + 1
          }
        }
      } else {
        sub(arr(7), arr(17))
        sub(arr(19), arr(29))
        if(arr.length > 31)
          sub(arr(31), arr(41))
        if(arr.length > 43)
          sub(arr(43), arr(53))

        def sub(uid: String, level: String): Unit = {
          if(!levelsMap.contains(uid))
            levelsMap(uid) = level.takeWhile(_ != '/').toInt
        }
      }
    }
  }

  println(result.toList.sortWith { case ((_, c1), (_, c2)) => c2 <= c1 }.mkString("\n"))
}
