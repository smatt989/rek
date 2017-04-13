package com.example.app.migrations

import com.example.app.models.{User}
import com.example.app.{AppGlobals, Tables}
import slick.driver.PostgresDriver.api._

trait Migration {

  def run: Unit
}

class Migration1() {

  val newSchemas = (Tables.thanks.schema)

  def run: Unit = {
    AppGlobals.db().run(DBIO.seq(newSchemas.create))
  }

}