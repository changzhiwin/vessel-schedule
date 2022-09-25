package zio.doreal.vessel.entities

import zio.json._

case class ScheduleStatus(
    shipId: String,
    fullName: String,
    inVoya: String,
    outVoya: String,
    pob: String,
    etb: String,
    etd: String,
    inAgent: String,
    outAgent: String,
    notes: String,

    // uuid, for relation mapping
    id: String = "") {

  def isSame(newest: ScheduleStatus): Boolean = {
    (pob == newest.pob) &&
    (etb == newest.etb) &&
    (etd == newest.etd) &&
    (notes == newest.notes) &&
    (shipId == newest.shipId) && 
    (fullName == newest.fullName) &&
    (inVoya == newest.inVoya) &&
    (outVoya == newest.outVoya) &&
    (inAgent == newest.inAgent) &&
    (outAgent == newest.outAgent)
  }
}

object ScheduleStatus {
  implicit val encodeScheduleStatus: JsonEncoder[ScheduleStatus] = DeriveJsonEncoder.gen[ScheduleStatus]
  implicit val decodeScheduleStatus: JsonDecoder[ScheduleStatus] = DeriveJsonDecoder.gen[ScheduleStatus]
}