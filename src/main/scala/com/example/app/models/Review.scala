package com.example.app.models

import com.example.app.{HasIntId, SlickDbObject, Tables, Updatable}
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

case class Review(id: Int, userId: Int, destinationId: Int, rating: Double, note: Option[String]) extends HasIntId[Review] {
  def updateId(id: Int) = this.copy(id = id)
  def toJson(user: UserJson) =
    ReviewJson(user, destinationId, rating, note)
}

case class ReviewJsonRequest(destinationId: Int, rating: Double, note: Option[String]) {
  def newReview(userId: Int) =
    Review(0, userId, destinationId, rating, note)
}

case class ReviewJson(user: UserJson, destinationId: Int, rating: Double, note: Option[String])

object Review extends Updatable[Review, (Int, Int, Int, Double, Option[String]), Tables.Reviews] {
  val table = Tables.reviews

  def reify(tuple: (Int, Int, Int, Double, Option[String])) =
    (apply _).tupled(tuple)

  def classToTuple(a: Review) =
    unapply(a).get

  def saveReviewByUserForDestination(userId: Int, review: ReviewJsonRequest) = {
    val preexisting = Await.result(getReviewByUserForDestination(userId, review.destinationId), Duration.Inf)
    val toSave = {
      preexisting match {
        case Some(p) => review.newReview(userId).updateId(p.id)
        case None => review.newReview(userId)
      }
    }
    save(toSave)
  }

  def getReviewsForUser(userId: Int) = {
    val users = Await.result(UserConnection.getBySenderId(userId), Duration.Inf).map(_.receiverUserId) :+ userId
    db.run(
      table.filter(a => a.userId inSet users).result
    ).map(_.map(reify))
  }

  def getReviewsForUserForDestination(userId: Int, destinationId: Int) = {
    val users = Await.result(UserConnection.getBySenderId(userId), Duration.Inf).map(_.receiverUserId) :+ userId
    db.run(
      table.filter(a => a.userId.inSet(users) && a.destinationId === destinationId).result
    ).map(_.map(reify))
  }

  def getReviewsByUser(userId: Int) = {
    db.run(table.filter(_.userId === userId).result).map(_.map(reify))
  }

  def getReviewByUserForDestination(userId: Int, destinationId: Int) =
    db.run(table.filter(a => a.userId === userId && a.destinationId === destinationId).result).map(_.headOption.map(reify))

  def updateQuery(a: Review) = table.filter(_.id === a.id)
    .map(x => (x.rating, x.note))
    .update((a.rating, a.note))
}