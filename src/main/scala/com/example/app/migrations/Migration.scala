package com.example.app.migrations

import com.example.app.models.{DeviceToken, Review, User}
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

class Migration3() {

  def run: Unit = {
    AppGlobals.db().run(
      sqlu"""
         ALTER TABLE REVIEWS
         ADD COLUMN RATING FLOAT NULL;

         UPDATE REVIEWS
         SET RATING = 4
         WHERE POSITIVE_RATING = TRUE;

         UPDATE REVIEWS
         SET RATING = 2
         WHERE POSITIVE_RATING = FALSE;

         ALTER TABLE REVIEWS
         DROP COLUMN POSITIVE_RATING;

         ALTER TABLE REVIEWS
         ALTER COLUMN RATING SET NOT NULL;"""
    )
  }

  def undo: Unit = {
    AppGlobals.db().run(
      sqlu"""
         alter table reviews
         ADD COLUMN POSITIVE_RATING BOOLEAN NULL;

         UPDATE REVIEWS
         SET POSITIVE_RATING = TRUE
         WHERE RATING >= 3;

         UPDATE REVIEWS
         SET POSITIVE_RATING = FALSE
         WHERE RATING < 3;

         ALTER TABLE reviews
         DROP COLUMN RATING;

         ALTER TABLE reviews
         ALTER COLUMN POSITIVE_RATING SET NOT NULL;"""
    )
  }
}