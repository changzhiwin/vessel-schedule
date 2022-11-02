package cc.knowship.subscribe.wharf

import zio._
import zio.json._
import zio.http.{Client, Request, URL}

import cc.knowship.subscribe.{SubscribeException, AppConfig}
import cc.knowship.subscribe.util.{Constants, TimeDateUtils}
import cc.knowship.subscribe.service.WharfInformationServ
import cc.knowship.subscribe.db.model.{Vessel, Voyage}

case class NpediWharfInformation(client: Client, config: AppConfig) extends WharfInformationServ {

  import SubscribeException._

  /**
    * 对比计划靠泊、计划离泊、开始进箱、结束进箱时间
    */
  override def isChanged(vAged: Voyage, vNew: Voyage): Boolean = {
    (vNew.eta != vAged.eta || vNew.etd != vAged.etd || vNew.rcvEnd != vAged.rcvEnd || vNew.rcvStart != vAged.rcvStart)
  }

  /**
    * 当前时间大于ctnEndTime，则判断这水船信息不在变更了
    * 假设都是基于东八区的时间
    * eg. 2022-10-31 00:00:01 < 2022-11-01 09:57:19
    */
  override def isFinished(vNew: Voyage): Boolean = {
    val currTimeStr = TimeDateUtils.currentLocalDateTimeStr
    (vNew.rcvEnd != Constants.DEFAULT_STRING_VALUE && vNew.rcvEnd < currTimeStr)
  }

  lazy val VoyageAPI = URL.fromString(config.npedi.scheUrl)

  // 不需要做处理，假定传过来的都是真实的
  override def voyageOfVessel(vesselName: String, voyageName: String): Task[String] = {
    ZIO.succeed(voyageName)
  }
  
  override def voyageStatus(vesselName: String, voyageCode: String): Task[(Vessel, Voyage)] = for {
    url      <- ZIO.fromEither(VoyageAPI)
                   .mapError(m => new URLParseFailed(s"$m"))
    
    response <- client.request(Request.get(url.setQueryParams(s"vessel=${vesselName}&voyage=${voyageCode}")))
    body     <- response.body.asString
    schReply <- ZIO.fromEither(body.fromJson[NpediScheduleInfoReply])
                   .mapError(_ => JsonDecodeFailed("NpediScheduleInfoReply"))
    result   <- ZIO.fromEither(buildModels(schReply.data))
  } yield result

  private def buildModels(scheduleData: NpediScheduleData): Either[SubscribeException, (Vessel, Voyage)] = {
    scheduleData.total match {
      case 1 => Right( formatingModels(scheduleData.list.head) )
      // size = 0 或者 size >= 2 都是异常
      case _           => Left( VoyageMustOnlyOne(s"scheduleData.list = ${scheduleData.list.map(bd => bd.vesselEnName + "/" + bd.voyage).mkString("[", ",", "]")}") )
    }
  }

  private def formatingModels(scheduleBody: NpediScheduleBody): (Vessel, Voyage) = {
    val s = scheduleBody

    val vessel = Vessel(
      shipCode = s.vesselCode,
      shipName = s.vesselEnName,
      shipCnName = s.vesselCnName.getOrElse(Constants.DEFAULT_STRING_VALUE),
      company = s.vesselOwner,
      unCode = s.vesselUnCode,
      inAgent = s.vesselAgentName.getOrElse(Constants.DEFAULT_STRING_VALUE),
      outAgent = s.vesselAgent.getOrElse(Constants.DEFAULT_STRING_VALUE),

      id = Constants.DEFAULT_UUID,
      wharfId = Constants.DEFAULT_UUID,
      createAt = Constants.DEFAULT_EPOCH_MILLI,
    )

    val rcv = s.published match {
      case "Y" => (s.ctnStartTime, s.ctnEndTime)
      case "N" => (Constants.DEFAULT_STRING_VALUE, Constants.DEFAULT_STRING_VALUE)
    }

    val voyage = Voyage(
      terminalCode = s.terminal,
      inVoy = s"${s.voyage}-I",
      outVoy = s.voyage,                                             // 以这个字段对齐其他码头
      serviceId = s.serviceCode.getOrElse(Constants.DEFAULT_STRING_VALUE),

      rcvStart = rcv._1,                                             // 进箱开始时间
      rcvEnd = rcv._2,                                               // 进箱结束时间 

      pob = s.publishTime.getOrElse(Constants.DEFAULT_STRING_VALUE), // 保存一下这个时间，因为判断和这个相关
      etb = Constants.DEFAULT_STRING_VALUE,
      
      eta = s.eta.getOrElse(Constants.DEFAULT_STRING_VALUE),         // 计划靠泊时间
      ata = s.ata.getOrElse(Constants.DEFAULT_STRING_VALUE),         // 实际靠泊时间
      etd = s.etd.getOrElse(Constants.DEFAULT_STRING_VALUE),         // 计划离泊时间
      atd = s.atd.getOrElse(Constants.DEFAULT_STRING_VALUE),         // 实际离泊时间

      notes = s.published,                                           // 用于判断是否有进箱信息了

      id = Constants.DEFAULT_UUID,
      vesselId = Constants.DEFAULT_UUID,
      createAt = Constants.DEFAULT_EPOCH_MILLI,
      updateAt = Constants.DEFAULT_EPOCH_MILLI,
    )

    (vessel, voyage)
  }
}

case class NpediScheduleBody(
  mmsi: Option[String],                // "352361000",
  vesselEnName: String,                // "MSC BETTINA",
  vesselCnName: Option[String],        // "地中海贝蒂娜",
  vesselCode: String,                  // "MSCET",
  vesselUnCode: String,                // "UN9399038",
  terminal: String,                    // "BLCT3",
  voyage: String,                      // "FK243A",
  vesselDirect: Option[String],        // "E",
  tradeFlag: Option[String],           // "W",
  vesselReference: Option[String],     // "MSCET1030",
  imo: Option[String],                 // "1",
  vesselSysid: Option[String],         // "0",
  vesselOwner: String,                 //"MSC",
  vesselAgent: Option[String],         // "PEN",
  serviceCode: Option[String],         // null, "serviceCode": "MSCMID",
  lineOperatorCode: String,            //"MSC",
  ctnStartTime: String,                //"2022-10-26 00:00:01", "9998-01-01 00:00:01"
  ctnEndTime: String,                  //"2022-10-31 00:00:01",  "9999-01-01 00:00:01"
  hazardStartTime: Option[String],     // null,
  hazardEndTime: Option[String],       // null,
  spHazardStartTime: Option[String],   // null,
  spHazardEndTime: Option[String],     // null,
  etaMonthly: Option[String],          // "2022-10-30 14:00:00",
  etdMonthly: Option[String],          // "2022-10-31 00:00:01",
  etaRecently: Option[String],         // null,
  etdRecently: Option[String],         // null,
  etaDaily: Option[String],            // "2022-10-31 16:30:00",
  etdDaily: Option[String],            // "2022-11-01 14:00:00",
  eta: Option[String],                 // "2022-10-31 16:30:00",
  etd: Option[String],                 // "2022-11-01 14:00:00",
  etanchor: Option[String],            // "2022-10-31 00:00:01",
  atanchor: Option[String],            // "2022-10-31 00:00:01",
  ata: Option[String],                 // null,
  atd: Option[String],                 // null,
  ats: Option[String],                 // null,
  ate: Option[String],                 // null,
  ets: Option[String],                 // null,
  ete: Option[String],                 // null,
  voycloseTime: Option[String],        // null,
  customCloseTime: Option[String],     // "2022-10-31 04:30:00",
  portCloseTime: Option[String],       // "2022-10-31 04:30:00",
  berthReference: Option[String],      // "10",
  ports: Option[String],               // "AEJEA:HKHKG: OMSLL: ...",
  lastPortCode: Option[String],        // "CNSHA",
  nextPortCode: Option[String],        // "CNSHK",
  lastPortName: Option[String],        // "上海",
  nextPortName: Option[String],        // "蛇口",
  voyagePublishTime: Option[String],   // null,
  voyagePublished: Option[String],     // "Y",
  publishTime: Option[String],         // "2022-10-25 09:22:57",
  published: String,                   // "Y",
  modifyCode: Option[String],          // null,
  modifyRemark: Option[String],        // null,
  vesselOwnerCtnTimes: Option[String], // null,
  status: Option[String],              // "Y",
  vesselAgentName: Option[String],     // "外代",
  etaBegin: Option[String],            // null
  etaEnd: Option[String],              // null
  ataBegin: Option[String],            // null
  ataEnd: Option[String]               // null
)

object NpediScheduleBody {
  implicit val codec: JsonCodec[NpediScheduleBody] = DeriveJsonCodec.gen
}

case class NpediScheduleData(pageNum: Int, pageSize: Int, total: Int, totalPages: Int, list: List[NpediScheduleBody])

object NpediScheduleData {
  implicit val codec: JsonCodec[NpediScheduleData] = DeriveJsonCodec.gen
}

case class NpediScheduleInfoReply(data: NpediScheduleData, code: Int, msg: String)

object NpediScheduleInfoReply {
  implicit val codec: JsonCodec[NpediScheduleInfoReply] = DeriveJsonCodec.gen
}