package zio.doreal.vessel.entities

import zio.json._

case class ScheduleStatus(
    terminalCode: String,          // TerminalCode
    shipCode: String,              // ShipId
    shipName: String,              // TheFullName
    company: String,               // LINEID
    imo: String,           // IMO
    inVoy: String,         // invoynbr
    outVoy: String,        // outvoynbr

    inService: String,     // InServiceId
    outService: String,    // OutServiceId
    inBusiVoy: String,     // INBUSINESSVOY
    outBusiVoy: String,    // OUTBUSINESSVOY
    inAgent: String,       // Inagent
    outAgent: String,      // Outagent

    eta: String,           // ETADate
    pob: String,           // POB
    etb: String,           // ETB
    etd: String,           // ETD
    ata: String,           // ATA
    atd: String,           // ATD 有这个属性后，可以不在关注动态

    notes: String,         // Notes

    // uuid, for relation mapping
    id: String = "") {

  def isNotChangeOrEnd(newest: ScheduleStatus): (Boolean, String) = {
    //val vesselInfo = s"${shipName}/${outVoy}" // [${vesselInfo}] - 

    if (atd != newest.atd || atd != "-") {
      (false, s"[ATD: ${newest.atd}]")
    } else if (etd != newest.etd) {
      (false, s"[ETD: ${newest.etd}]")
    } else if (etb != newest.etb) {
      (false, s"[ETB: ${newest.etb}]")
    } else {
      (true, "-")
    }
  }
}

object ScheduleStatus {
  implicit val encodeScheduleStatus: JsonEncoder[ScheduleStatus] = DeriveJsonEncoder.gen[ScheduleStatus]
  implicit val decodeScheduleStatus: JsonDecoder[ScheduleStatus] = DeriveJsonDecoder.gen[ScheduleStatus]
}