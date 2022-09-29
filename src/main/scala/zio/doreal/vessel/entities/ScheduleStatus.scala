package zio.doreal.vessel.entities

import zio.json._

case class ScheduleStatus(
    terminalCode: String,          // TerminalCode
    shipCode: String,              // ShipId
    shipName: String,              // TheFullName
    company: String,               // LINEID
    imo: Option[String],           // IMO
    inVoy: String,         // invoynbr
    outVoy: String,        // outvoynbr

    inService: Option[String],     // InServiceId
    outService: Option[String],    // OutServiceId
    inBusiVoy: Option[String],     // INBUSINESSVOY
    outBusiVoy: Option[String],    // OUTBUSINESSVOY
    inagent: Option[String],       // Inagent
    outagent: Option[String],      // Outagent

    eta: Option[String],           // ETADate
    pob: Option[String],           // POB
    etb: Option[String],           // ETB
    etd: Option[String],           // ETD
    ata: Option[String],           // ATA
    atd: Option[String],           // ATD 有这个属性后，可以不在关注动态

    notes: Option[String],         // Notes

    // uuid, for relation mapping
    id: String = "") {

  def isNotChangeOrEnd(newest: ScheduleStatus): Boolean = {
    (eta == newest.eta) &&
    (pob == newest.pob) &&
    (etb == newest.etb) &&
    (etd == newest.etd) &&
    (ata == newest.ata) &&
    (atd == newest.atd) &&
    (notes == newest.notes) && atd.isEmpty
  }
}

object ScheduleStatus {
  implicit val encodeScheduleStatus: JsonEncoder[ScheduleStatus] = DeriveJsonEncoder.gen[ScheduleStatus]
  implicit val decodeScheduleStatus: JsonDecoder[ScheduleStatus] = DeriveJsonDecoder.gen[ScheduleStatus]
}