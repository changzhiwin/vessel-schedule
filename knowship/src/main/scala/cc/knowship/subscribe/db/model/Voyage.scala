package cc.knowship.subscribe.db.model

import java.util.UUID

import zio.json._

final case class Voyage(
  id: UUID,

  terminalCode: String,  // TerminalCode 港区
  inVoy: String,         // invoynbr
  outVoy: String,        // outvoynbr
  serviceId: String,     // ServiceId

  rcvStart: String,      // Npedi#ctnstart
  rcvEnd: String,        // Npedi#ctnend

  eta: String,           // ETADate
  pob: String,           // POB
  etb: String,           // ETB
  etd: String,           // ETD
  ata: String,           // ATA
  atd: String,           // ATD 有这个属性后，可以不在关注动态

  notes: String,         // Notes，附言

  vesselId: UUID,
  createAt: Long, 
  updateAt: Long
)

object Voyage {
  implicit val codec: JsonCodec[Voyage] = DeriveJsonCodec.gen
}
