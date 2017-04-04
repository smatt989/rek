package com.example.app.models

import com.example.app.{HasIntId, SlickDbObject, Tables}
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
  * Created by matt on 3/29/17.
  */
case class Destination(id: Int = 0, name: String, address: String, latitude: Double, longitude: Double) extends HasIntId[Destination] {
  def updateId(id: Int) = this.copy(id = id)
}

object Destination extends SlickDbObject[Destination, (Int, String, String, Double, Double), Tables.Destinations] {
  val table = Tables.destinations

  def reify(tuple: (Int, String, String, Double, Double)) =
    (apply _).tupled(tuple)

  def classToTuple(a: Destination) =
    unapply(a).get

  def getDestinationLookup(lookup: Destination) = {
    val alreadyExists = Await.result(byDestinationLookup(lookup), Duration.Inf)
    alreadyExists.getOrElse{
      Await.result(create(lookup), Duration.Inf)
    }
  }

  def byDestinationLookup(lookup: Destination) =
    db.run(table.filter(a => a.name === lookup.name && a.address === lookup.address).result).map(_.headOption.map(reify))
}