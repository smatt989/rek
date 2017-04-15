package com.example.app.migrations

import com.example.app.models.{DeviceToken, User}
import com.example.app.{AppGlobals, Tables}
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait Migration {

  def run: Unit
}

class Migration1() {

  val newSchemas = (Tables.thanks.schema)

  def run: Unit = {
    AppGlobals.db().run(DBIO.seq(newSchemas.create))
  }

}

class Migration2() {

  def run: Unit = {
    val tokens = Await.result(DeviceToken.getAll, Duration.Inf)
    AppGlobals.db().run(
      User.table.filterNot(_.id inSet tokens.map(_.userId)).delete
    )
  }
}