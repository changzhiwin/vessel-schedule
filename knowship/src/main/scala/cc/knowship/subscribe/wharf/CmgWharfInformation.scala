package cc.knowship.subscribe.wharf

import zio._
import zio.json._
import zio.http.{Client, Request, URL}

import cc.knowship.subscribe.SubscribeException
import cc.knowship.subscribe.util.Constants
import cc.knowship.subscribe.service.WharfInformationServ
import cc.knowship.subscribe.db.model.{Vessel, Voyage}

case class CmgWharfInformation(client: Client) extends WharfInformationServ {

  import SubscribeException._

  lazy val VesselAPI = URL.fromString("http://eportapisct.scctcn.com/api/GVesselVoyage")

  lazy val VoyageAPI = URL.fromString("http://eportapisct.scctcn.com/api/VesselSchedule")

  override def voyageOfVessel(vesselName: String, voyageName: String): Task[String] = for {
    url      <- ZIO.fromEither(VesselAPI)
                   .mapError(m => new URLParseFailed(s"$m"))

    response <- client.request(Request.get(url.setQueryParams(s"VesselName=${vesselName}&PageIndex=1&PageSize=999")))
    body     <- response.body.asString
    voReply  <- ZIO.fromEither(body.fromJson[VoyageReply])
                   .mapError(_ => JsonDecodeFailed("VoyageReply"))
    voyage   <- ZIO.fromEither( chooseVoyage(voReply.InnerList, voyageName) )
  } yield voyage

  override def voyageStatus(vesselName: String, voyageCode: String): Task[(Vessel, Voyage)] = for {
    url      <- ZIO.fromEither(VoyageAPI)
                   .mapError(m => new URLParseFailed(s"$m"))
    
    response <- client.request(Request.get(url.setQueryParams(s"FullName=${vesselName}&vesselName=${vesselName}&OutboundVoy=${voyageCode}&PageIndex=1&PageSize=30")))
    body     <- response.body.asString
    schReply <- ZIO.fromEither(body.fromJson[ScheduleInfoReply])
                   .mapError(_ => JsonDecodeFailed("ScheduleInfoReply"))
    result   <- ZIO.fromEither(buildModels(schReply.InnerList))
  } yield result

  private def buildModels(scheduleList: List[ScheduleInfo]): Either[SubscribeException, (Vessel, Voyage)] = {
    scheduleList match {
      case head :: Nil => Right( formatingModels(head) )
      // size = 0 或者 size >= 2 都是错误的
      case _           => Left( VoyageMustOnlyOne(s"ScheduleInfoList = ${scheduleList.map(info => info.TheFullName + "/" + info.outvoynbr).mkString("[", ",", "]")}") )
    }
  }

  private def formatingModels(scheduleInfo: ScheduleInfo): (Vessel, Voyage) = {
    val s = scheduleInfo

    val vessel = Vessel(
      shipCode = s.ShipId,
      shipName = s.TheFullName,
      shipCnName = Constants.DEFAULT_STRING_VALUE,
      company = s.LINEID,
      unCode = s.IMO.getOrElse(Constants.DEFAULT_STRING_VALUE),
      inAgent = s.Inagent.getOrElse(Constants.DEFAULT_STRING_VALUE),
      outAgent = s.Outagent.getOrElse(Constants.DEFAULT_STRING_VALUE),

      id = Constants.DEFAULT_UUID,
      wharfId = Constants.DEFAULT_UUID,
      createAt = Constants.DEFAULT_EPOCH_MILLI,
    )
    val voyage = Voyage(
      terminalCode = s.TerminalCode,
      inVoy = s.invoynbr,
      outVoy = s.outvoynbr,
      serviceId = s.ServiceId.getOrElse(Constants.DEFAULT_STRING_VALUE), // ++

      rcvStart = Constants.DEFAULT_STRING_VALUE, // 进箱开始时间 Npedi ++
      rcvEnd = Constants.DEFAULT_STRING_VALUE,   // 进箱结束时间 Npedi ++

      eta = s.ETADate.getOrElse(Constants.DEFAULT_STRING_VALUE),
      pob = s.POB.getOrElse(Constants.DEFAULT_STRING_VALUE),
      etb = s.ETB.getOrElse(Constants.DEFAULT_STRING_VALUE),
      etd = s.ETD.getOrElse(Constants.DEFAULT_STRING_VALUE),
      ata = s.ATA.getOrElse(Constants.DEFAULT_STRING_VALUE),
      atd = s.ATD.getOrElse(Constants.DEFAULT_STRING_VALUE),
      notes = s.Notes.getOrElse(Constants.DEFAULT_STRING_VALUE),

      id = Constants.DEFAULT_UUID,
      vesselId = Constants.DEFAULT_UUID,
      createAt = Constants.DEFAULT_EPOCH_MILLI,
      updateAt = Constants.DEFAULT_EPOCH_MILLI,
    )

    (vessel, voyage)
  }

  private def chooseVoyage(voyageList: List[VoyageDetail], voyageName: String): Either[SubscribeException, String] = {
    voyageList match {
      case Nil          => Left( VesselNoVoyageFound(s"voyageList is empty, with name #${voyageName}") )
      case only :: Nil  => Right(only.OutBoundVoy) 
      case head :: tail => Right( if (tail.exists(v => v.OutBoundVoy == voyageName)) voyageName else head.OutBoundVoy )
    }
  }
}

case class VoyageDetail(TerminalCode: String, OutBoundVoy: String, InBoundVoy: String)

object VoyageDetail {
  implicit val codec: JsonCodec[VoyageDetail] = DeriveJsonCodec.gen
}

case class VoyageReply(InnerList: List[VoyageDetail], TotalPages: Int, TotalCount: Int, PageIndex: Int, PageSize: Int)

object VoyageReply {
  implicit val codec: JsonCodec[VoyageReply] = DeriveJsonCodec.gen
}

case class ScheduleInfo(
  TerminalCode: String,           //"CCT",
  ETADate: Option[String],        //"2022-09-20 11:00",
  ServiceId: Option[String],      //"IFX",
  InServiceId: Option[String],    //"IFX",
  OutServiceId: Option[String],   //"IFX",
  ShipId: String,                 //"XTJN",
  TheFullName: String,            //"XIN TIAN JIN",
  LINEID: String,                 //"COS",
  invoynbr: String,               //"076WI",
  outvoynbr: String,              //"076W",
  POB: Option[String],            //"2022-09-21 00:30",
  ETB: Option[String],            //"2022-09-21",
  ETD: Option[String],            //"2022-09-21 19:30",
  ATA: Option[String],            //null,
  ATD: Option[String],            //null, // 有这个属性后，可以不在关注动态
  Notes: Option[String],          //"已开始收箱",
  IMO: Option[String],            //"UN9234343",
  INBUSINESSVOY: Option[String],  //"076WI",
  OUTBUSINESSVOY: Option[String], //"076W",
  Inagent: Option[String],        //"COS",
  Outagent: Option[String]        //"COS"
)

object ScheduleInfo {
  implicit val codec: JsonCodec[ScheduleInfo] = DeriveJsonCodec.gen
}

case class ScheduleInfoReply(InnerList: List[ScheduleInfo], TotalPages: Int, TotalCount: Int, PageIndex: Int, PageSize: Int)

object ScheduleInfoReply {
  implicit val codec: JsonCodec[ScheduleInfoReply] = DeriveJsonCodec.gen
}