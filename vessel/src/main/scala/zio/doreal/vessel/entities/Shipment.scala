package zio.doreal.vessel.entities

import zio.doreal.vessel.utils.TypeTransform

case class Shipment(id: String, createAt: Long, updateAt: Long, notifyAt: Long, queryKey: String, vessel: String, voyage: String, wharf: String, scheduleStatusId: String) {

  def debugTimestamp: String = s"${TypeTransform.epochMilliToString(createAt)} | ${TypeTransform.epochMilliToString(updateAt)} | ${TypeTransform.epochMilliToString(notifyAt)}"

  def debugPrint: String = s"${id} | ${queryKey} | ${vessel} | ${voyage} | ${wharf} | ${scheduleStatusId} | ${debugTimestamp}"
}