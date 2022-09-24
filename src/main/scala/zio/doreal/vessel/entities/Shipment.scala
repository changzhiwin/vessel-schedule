package zio.doreal.vessel.entities

import zio.doreal.vessel.utils.TypeTransform

case class Shipment(id: String, createAt: Long, updateAt: Long, notifyAt: Long, queryKey: String, scheduleStatusId: String) {

  def debugPrint: String = s"${id} | ${queryKey} | ${scheduleStatusId} | ${TypeTransform.epochMilliToString(createAt)} | ${TypeTransform.epochMilliToString(updateAt)} | ${TypeTransform.epochMilliToString(notifyAt)}"
}