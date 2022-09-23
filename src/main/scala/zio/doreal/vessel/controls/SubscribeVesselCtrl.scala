package zio.doreal.vessel.controls

import zio._
import zio.http._

trait SubscribeVesselCtrl {

  def handle(req: Request): ZIO[Any, Throwable, Response]
}

object SubscribeVesselCtrl {
  
  def handle(req: Request): ZIO[SubscribeVesselCtrl, Throwable, Response] = 
    ZIO.serviceWithZIO[SubscribeVesselCtrl](_.handle(req))
}
