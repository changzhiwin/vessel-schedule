package zio.doreal.vessel.entities

case class Shipment(id: String, createdAt: String, vessel: String, voyage: String, wharf: String = "CMG")