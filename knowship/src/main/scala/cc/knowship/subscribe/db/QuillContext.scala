package cc.knowship.subscribe.db

import io.getquill.jdbczio.Quill
import io.getquill._

object QuillContext extends SqliteZioJdbcContext(SnakeCase) {

  val dataSourceLayer = Quill.DataSource.fromPrefix("database").orDie
}