package com.example.app.models

import com.example.app.{HasIntId, SlickDbObject, Tables}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class Thank(id: Int = 0, senderUserId: Int, receiverUserId: Int, destinationId: Int) extends HasIntId[Thank]  {
  def updateId(id: Int) = this.copy(id = id)
}

case class ThankJsonRequest(receiverUserId: Int, destinationId: Int) {
  def toModel(senderUserId: Int) =
    Thank(0, senderUserId, receiverUserId, destinationId)
}

object Thank extends SlickDbObject[Thank, (Int, Int, Int, Int), Tables.Thanks] {
  lazy val table = Tables.thanks

  def reify(tuple: (Int, Int, Int, Int)) =
    (apply _).tupled(tuple)

  def classToTuple(a: Thank) =
    unapply(a).get

  def makeAThank(thank: Thank) = {
    val existing = Await.result(findThankByThank(thank), Duration.Inf)
    existing.getOrElse(Await.result(create(thank), Duration.Inf))
  }

  def allThanksReceived(receiverUserId: Int) =
    db.run(table.filter(_.receiverUserId === receiverUserId).result).map(_.map(reify))

  def thanksSentByUserForDestinations(senderUserId: Int, destinationIds: Seq[Int]) = {
    db.run(table.filter(a => a.destinationId.inSet(destinationIds) && a.senderUserId === senderUserId).result).map(_.map(reify))
  }

  def thanksReceivedByUserForDestinations(receiverUserId: Int, destinationIds: Seq[Int]) = {
    db.run(table.filter(a => a.destinationId.inSet(destinationIds) && a.receiverUserId === receiverUserId).result).map(_.map(reify))
  }

  def thanksForUserAndDestinations(userId: Int, destinationIds: Seq[Int]) = {
    db.run(table.filter(a => a.destinationId.inSet(destinationIds) && (a.senderUserId === userId || a.receiverUserId === userId)).result).map(_.map(reify))
  }

  private[this] def findThankByThank(thank: Thank) = {
    db.run(
      table.filter(a => a.senderUserId === thank.senderUserId && a.receiverUserId === thank.receiverUserId && a.destinationId === thank.destinationId).result
    ).map(_.headOption.map(reify))
  }
}