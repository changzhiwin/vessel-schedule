package cc.knowship.subscribe.wharf

import zio._
import zio.json._
import zio.http.{Client, Request, URL}

import cc.knowship.subscribe.service.WharfInfoServ
import cc.knowship.subscribe.db.model.{Vessel, Voyage}

case class CmgWharfInformation(client: Client) extends WharfInfoServ {

  lazy val VesselAPI = URL.fromString("http://eportapisct.scctcn.com/api/GVesselVoyage")

  lazy val VoyageAPI = URL.fromString("http://eportapisct.scctcn.com/api/VesselSchedule")

  override def voyageOfVessel(vesselName: String, voyageChoose: Option[String]): Task[Option[String]] = for {
    url      <- ZIO.fromEither(VesselAPI).mapError(m => new Throwable(m))
    response <- client.request(Request(url = url.setQueryParams(s"VesselName=${vesselName}&PageIndex=1&PageSize=999")))
    body     <- response.body.asString
    voReply  <- ZIO.fromEither(body.fromJson[VoyageReply].mapError(m => new Throwable(m)))
  } yield chooseVoyage(voReply.InnerList)

  private def chooseVoyage(voyageList: List[VoyageDetail]): Option[String] = {
    voyageList match {
      case Nil          => None
      case only :: Nil  => Some(only.OutBoundVoy) 
      case head :: tail => if (tail.exists(v => v.OutBoundVoy == voyageChoose)) voyageChoose else head.OutBoundVoy
    }
  }

  override def voyageStatus(vesselName: String, voyageCode: String): Task[(Vessel, Voyage)] = for {
    url      <- ZIO.fromEither(VoyageAPI).mapError(m => new Throwable(m))
    response <- client.request(Request(url = url.setQueryParams(s"FullName=${vesselName}&vesselName=${vesselName}&OutboundVoy=${voyageCode}&PageIndex=1&PageSize=30")))
    body     <- response.body.asString
    schReply <- ZIO.fromEither(body.fromJson[ScheduleInfoReply]).mapError(m => new Throwable(m))
  } yield buildModels(schReply.InnerList)

  private def buildModels(scheduleList: List[ScheduleInfo]): Option[(Vessel, Voyage)] = {
    scheduleList match {
      case head :: Nil => 
      case _           => None
    }
  }

  private def formatModels(scheduleInfo: ScheduleInfo): (Vessel, Voyage) = {
    Vessel(
      shipCode = s.ShipId,
      shipName = s.TheFullName,
      company = s.LINEID,
      imo = s.IMO.getOrElse("-")
    )

    Voyage(
      terminalCode = s.TerminalCode,
      inVoy = s.invoynbr,
      outVoy = s.outvoynbr,
      inService = s.InServiceId.getOrElse("-"),
      outService = s.OutServiceId.getOrElse("-"),
      inBusiVoy = s.INBUSINESSVOY.getOrElse("-"),
      outBusiVoy = s.OUTBUSINESSVOY.getOrElse("-"),
      inAgent = s.Inagent.getOrElse("-"),
      outAgent = s.Outagent.getOrElse("-"),

      eta = s.ETADate.getOrElse("-"),
      pob = s.POB.getOrElse("-"),
      etb = s.ETB.getOrElse("-"),
      etd = s.ETD.getOrElse("-"),
      ata = s.ATA.getOrElse("-"),
      atd = s.ATD.getOrElse("-"),
      notes = s.Notes.getOrElse("-")
    )

  }
}

case class VoyageDetail(TerminalCode: String, OutBoundVoy: String, InBoundVoy: String)

case class VoyageReply(InnerList: List[VoyageDetail], TotalPages: Int, TotalCount: Int, PageIndex: Int, PageSize: Int)

case class ScheduleInfo(
  TerminalCode: String, //"CCT",
  ETADate: Option[String], //"2022-09-20 11:00",
  ServiceId: Option[String], //"IFX",
  InServiceId: Option[String], //"IFX",
  OutServiceId: Option[String], //"IFX",
  ShipId: String, //"XTJN",
  TheFullName: String, //"XIN TIAN JIN",
  LINEID: String, //"COS",
  invoynbr: String, //"076WI",
  outvoynbr: String, //"076W",
  POB: Option[String], //"2022-09-21 00:30",
  ETB: Option[String], //"2022-09-21",
  ETD: Option[String], //"2022-09-21 19:30",
  ATA: Option[String], //null,
  ATD: Option[String], //null, // 有这个属性后，可以不在关注动态
  Notes: Option[String], //"已开始收箱",
  IMO: Option[String], //"UN9234343",
  INBUSINESSVOY: Option[String], //"076WI",
  OUTBUSINESSVOY: Option[String], //"076W",
  Inagent: Option[String], //"COS",
  Outagent: Option[String] //"COS"
)

case class ScheduleInfoReply(InnerList: List[ScheduleInfo], TotalPages: Int, TotalCount: Int, PageIndex: Int, PageSize: Int)