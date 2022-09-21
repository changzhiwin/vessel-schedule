package zio.doreal.vessel.cmg

import zio._
import zio.json._
import zio.http.{Client, Request, URL}

import zio.doreal.vessel.{ScheduleStatus, ResponseScheduleStatus, VesselService}
import zio.doreal.vessel.cmg.models._

case class CMGVesselService(client: Client) extends VesselService {

  override def fetchScheduleStatus(vessel: String, voyage: Option[String]): Task[ResponseScheduleStatus] = {
    // Rewrite
    for {
      voyageResp   <- fetchVoyage(vessel)

      respSchedule <- if (0 == voyageResp.TotalCount) ZIO.succeed(ResponseScheduleStatus(message = s"No such vessel [${vessel}]")) else {
          val outBoundVoyList = voyageResp.InnerList.map(_.OutBoundVoy)
          val checkedVoyage = voyage match {
            case Some(v) => if (outBoundVoyList.contains(v)) v else outBoundVoyList.head
            case None    => outBoundVoyList.head
          }

          for {
            scheduleResp <- fetchSchedule(vessel, checkedVoyage)

            ret = scheduleResp.TotalCount match {
              case 0 => ResponseScheduleStatus(message = s"No such voyage [${vessel}]-[$checkedVoyage]")
              case 1 => {
                val sche = CmgSchedule.toScheduleStatus(scheduleResp.InnerList.head)
                ResponseScheduleStatus(status = Some(sche))
              }
              case _ => ResponseScheduleStatus(message = s"Find more than one schedules [${scheduleResp.toJsonPretty}]")
            }
          } yield ret

        }
    } yield respSchedule
  }

  private def fetchVoyage(vessel: String): Task[ResponseVoyage] = for {

    url        <- ZIO.fromEither(URL.fromString("http://eportapisct.scctcn.com/api/GVesselVoyage")).orDie
    response   <- client.request(Request(url = url.setQueryParams(s"VesselName=${vessel}&PageIndex=1&PageSize=999")))
    voyagesStr <- response.body.asString
    voyages    <- ZIO.fromEither(voyagesStr.fromJson[ResponseVoyage]).mapError(m => new Throwable(m))
  } yield voyages
  
  private def fetchSchedule(vessel: String, voyage: String): Task[ResponseSchedule] = for {
    url          <- ZIO.fromEither(URL.fromString("http://eportapisct.scctcn.com/api/VesselSchedule")).orDie
    response     <- client.request(Request(url = url.setQueryParams(s"FullName=${vessel}&vesselName=${vessel}&OutboundVoy=${voyage}&PageIndex=1&PageSize=30")))
    schedulesStr <- response.body.asString
    schedules    <- ZIO.fromEither(schedulesStr.fromJson[ResponseSchedule]).mapError(m => new Throwable(m))
  } yield schedules
}

object CMGVesselService {

  val live: ZLayer[Client, Nothing, VesselService] = ZLayer {
    for {
      client <- ZIO.service[Client]
    } yield CMGVesselService(client)
  }
}