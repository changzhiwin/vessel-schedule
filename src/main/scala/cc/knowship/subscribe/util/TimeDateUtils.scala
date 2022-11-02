package cc.knowship.subscribe.util

// JDK 7
//import java.text.SimpleDateFormat
//import java.util.Date

// JKD 8+
// From: https://aws.plainenglish.io/simple-tutorial-for-handling-date-and-time-in-scala-apache-spark-481d1e49763d
// LocalDateTime to pochMilli: https://stackoverflow.com/questions/23944370/how-to-get-milliseconds-from-localdatetime-in-java-8
// pochMilli to LocalDateTime: https://howtoprogram.xyz/2017/02/11/convert-milliseconds-localdatetime-java/
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalTime, LocalDateTime, ZoneId, ZoneOffset}

object TimeDateUtils {

  def localDateTimeToStr(dt: LocalDateTime, format: String = "yyyy-MM-dd HH:mm:ss"): String = DateTimeFormatter.ofPattern(format).format(dt)

  def epochMilliToLocalDateTime(epochMilli: Long, zone: String = "Asia/Shanghai"): LocalDateTime = Instant.ofEpochMilli(epochMilli).atZone(ZoneId.of(zone)).toLocalDateTime()

  def epochMilliToString(epochMilli: Long, zone: String = "Asia/Shanghai"): String = localDateTimeToStr(epochMilliToLocalDateTime(epochMilli, zone))

  def currentLocalDateTimeStr: String = localDateTimeToStr(LocalDateTime.now()) // epochMilliToString(System.currentTimeMillis())

  def isWorkingTime(when: Long, offsetStart: Long, offsetEnd: Long): Boolean = {
    // 取当天凌晨零点进行对比
    val ldt = LocalDateTime.of(LocalDate.now(ZoneId.of("Asia/Shanghai")), LocalTime.of(0, 0, 0))
    val startOfDayInMilli = ldt.toEpochSecond(ZoneOffset.of("+08")) * 1000

    (startOfDayInMilli + offsetStart <= when) && ( when < startOfDayInMilli + offsetEnd)
  }
}