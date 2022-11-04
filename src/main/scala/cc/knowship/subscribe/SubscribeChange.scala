package cc.knowship.subscribe

import cc.knowship.subscribe.db.model.{Subscription}

sealed trait SubscribeChange {
  def title: String
  def subscription: Subscription
}

object SubscribeChange {

  case class Update(changedInfo: String, val subscription: Subscription) extends SubscribeChange {
    val title = s"Updated, ${changedInfo}"

    //def subscription = Some(subcpt)
  }

  case class Finish(lastInfo: String, val subscription: Subscription) extends SubscribeChange {
    val title = s"Finished, ${lastInfo}"

    //def subscription = Some(subcpt)
  }

  /*
  case object Registe extends SubscribeChange {
    val title: String = "Subscribe Success"
  }

  case object Cancel extends SubscribeChange {
    val title: String = "Unsubscribe Success"
  }
  */
}