/**
  * 招商码头
  */
package zio.doreal.vessel.wharf.cmg

import zio._
import zio.json._
import zio.http.{Client, Request, URL}

import zio.doreal.vessel.wharf.{ScheduleStatusReply, VesselService}
import zio.doreal.vessel.entities.ScheduleStatus
import zio.doreal.vessel.wharf.cmg.models._

case class CMGVesselService(client: Client) extends VesselService {

  override def fetchScheduleStatus(vessel: String, voyage: String): Task[ScheduleStatusReply] = {
    // Rewrite
    for {
      voyageResp   <- fetchVoyage(vessel)

      respSchedule <- if (0 == voyageResp.TotalCount) ZIO.succeed(ScheduleStatusReply(code = -1, message = s"No such vessel [${vessel}]")) else {
          val outBoundVoyList = voyageResp.InnerList.map(_.OutBoundVoy)
          // if not correct, use the newly one
          val checkedVoyage = if (outBoundVoyList.contains(voyage)) voyage else outBoundVoyList.head

          for {
            scheduleResp <- fetchSchedule(vessel, checkedVoyage)

            ret = scheduleResp.TotalCount match {
              case 0 => ScheduleStatusReply(code = -1, message = s"No such voyage [${vessel}]-[$checkedVoyage]")
              case 1 => {
                val sche = CMGVesselService.transformToEntity(scheduleResp.InnerList.head)
                ScheduleStatusReply(status = Some(sche))
              }
              case _ => ScheduleStatusReply(code = -1, message = s"Find more than one schedules, please check your input. [${scheduleResp.toJsonPretty}]")
            }
          } yield ret

        }
    } yield respSchedule
  }

  private def fetchVoyage(vessel: String): Task[VoyageReply] = for {

    url        <- ZIO.fromEither(URL.fromString("http://eportapisct.scctcn.com/api/GVesselVoyage")).orDie
    response   <- client.request(Request(url = url.setQueryParams(s"VesselName=${vessel}&PageIndex=1&PageSize=999")))
    voyagesStr <- response.body.asString
    voyages    <- ZIO.fromEither(voyagesStr.fromJson[VoyageReply]).mapError(m => new Throwable(m))
  } yield voyages
  
  private def fetchSchedule(vessel: String, voyage: String): Task[ScheduleInfoReply] = for {
    url          <- ZIO.fromEither(URL.fromString("http://eportapisct.scctcn.com/api/VesselSchedule")).orDie
    response     <- client.request(Request(url = url.setQueryParams(s"FullName=${vessel}&vesselName=${vessel}&OutboundVoy=${voyage}&PageIndex=1&PageSize=30")))
    schedulesStr <- response.body.asString
    schedules    <- ZIO.fromEither(schedulesStr.fromJson[ScheduleInfoReply]).mapError(m => new Throwable(m))
  } yield schedules
}

object CMGVesselService {

  val live: ZLayer[Client, Nothing, VesselService] = ZLayer {
    for {
      client <- ZIO.service[Client]
    } yield CMGVesselService(client)
  }

  // helper
  def transformToEntity(s: ScheduleInfo): ScheduleStatus = ScheduleStatus(
      shipId = s.ShipId,
      fullName = s.TheFullName,
      inVoya = s.INBUSINESSVOY,
      outVoya = s.OUTBUSINESSVOY,
      pob = s.POB,
      etb = s.ETB,
      etd = s.ETD,
      inAgent = s.Inagent,
      outAgent = s.Outagent,
      notes = s.Notes)
}