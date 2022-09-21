package zio.doreal.vessel.entities

/**
  * @param id, uuid
  * @param openId, email address, wx id
  * @param channel, EMAIL/WX
  * @param parent, source address, receiver
  */
case class User(id: String, openId: String, channel: String, parent: String = "")