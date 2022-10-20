package cc.knowship.subscribe.wharf

import java.util.concurrent.TimeUnit

import zio._

import cc.knowship.subscribe.util.{Constants, TimeDateUtils}
import cc.knowship.subscribe.service.WharfInfoServ
import cc.knowship.subscribe.db.model.{Vessel, Voyage}

case class FakeWharfInformation() extends WharfInfoServ {

  def voyageOfVessel(vesselName: String, voyageChoose: Option[String]): Task[String] = {
    val code = voyageChoose match {
      case Some(v) => s"${v}-ok"
      case None    => "lastest"
    }
    ZIO.succeed(code)
  }

  def voyageStatus(vesselName: String, voyageCode: String): Task[(Vessel, Voyage)] = {
    val vessel = Vessel(
      shipCode = "shipCode",
      shipName = vesselName,
      company = "Fake Company",
      imo = "imo",

      id = Constants.DEFAULT_UUID,
      wharfId = Constants.DEFAULT_UUID,
      createAt = Constants.DEFAULT_EPOCH_MILLI,
    )

    val dateTimeStr = TimeDateUtils.currentLocalDateTimeStr

    val voyage = Voyage(
      terminalCode = "terminalCode",
      inVoy = s"${voyageCode}I",
      outVoy = voyageCode,
      inService = "inService",
      outService = "outService",
      inBusiVoy = s"${voyageCode}I",
      outBusiVoy = voyageCode,
      inAgent = "inAgent",
      outAgent = "outAgent",

      eta = dateTimeStr,
      pob = dateTimeStr,
      etb = dateTimeStr,
      etd = dateTimeStr,
      ata = dateTimeStr,
      atd = dateTimeStr,
      notes = "notes",

      id = Constants.DEFAULT_UUID,
      vesselId = Constants.DEFAULT_UUID,
      createAt = Constants.DEFAULT_EPOCH_MILLI,
      updateAt = Constants.DEFAULT_EPOCH_MILLI,
    )

    for {
      attr <- Random.nextIntBetween(0, 4).map(CareAttribute.apply(_))
      now   <- Clock.currentTime(TimeUnit.MILLISECONDS)
    } yield {
      val timeStr = TimeDateUtils.epochMilliToString(now)

      // give some changed
      val updatedVoyage = attr match {
        case CareAttribute.ETD => voyage.copy(etd = timeStr)
        case CareAttribute.ETB => voyage.copy(etb = timeStr)
        case CareAttribute.ATD => voyage.copy(atd = timeStr)
      }

      (vessel, updatedVoyage)
    }
  }

}

sealed trait CareAttribute

object CareAttribute {

  def apply(n: Int): CareAttribute = n match {
    case 0      => ATD
    case 1      => ETD
    case _: Int => ETB
  }

  final object ETD extends CareAttribute
  final object ETB extends CareAttribute
  final object ATD extends CareAttribute
}