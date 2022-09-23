package zio.doreal.vessel.entities

import java.text.SimpleDateFormat
import java.util.Date

case class Shipment(id: String, createAt: Long, updateAt: Long, queryKey: String, scheduleStatusId: String) {

  def getCreateTime: String = dateFormatStr(createAt)

  def getUpdateTime: String = dateFormatStr(updateAt)

  private def dateFormatStr(ms: Long, format: String = "yyyy-MM-dd HH:mm:ss"): String = (new SimpleDateFormat(format)).format(new Date(ms))

  def debugPrint: String = s"${id} | ${queryKey} | ${scheduleStatusId} | ${getCreateTime} | ${getUpdateTime}"
}