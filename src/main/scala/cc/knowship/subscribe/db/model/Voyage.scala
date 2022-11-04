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

  pob: String,           // POB
  etb: String,           // ETB

  eta: String,           // ETADate  计划靠泊时间
  ata: String,           // ATA      实际靠泊时间
  etd: String,           // ETD      计划离泊时间
  atd: String,           // ATD      实际离泊时间，有这个属性后，默认不在关注动态

  notes: String,         // Notes，附言

  vesselId: UUID,
  createAt: Long, 
  updateAt: Long
)

object Voyage {
  implicit val codec: JsonCodec[Voyage] = DeriveJsonCodec.gen
}
