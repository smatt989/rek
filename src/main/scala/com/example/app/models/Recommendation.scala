package com.example.app.models

import com.example.app.{HasIntId, PushNotificationManager, SlickDbObject, Tables}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by matt on 3/29/17.
  */
case class Recommendation(id: Int, senderUserId: Int, receiverUserId: Int, destinationId: Int, note: Option[String]) extends HasIntId[Recommendation] {
  def updateId(id: Int) = this.copy(id = id)
}

case class RecommendationJsonRequest(receiverUserId: Int, destinationId: Int, note: Option[String]){
  def newRecommendation(senderUserId: Int) =
    Recommendation(0, senderUserId, receiverUserId, destinationId, note)
}

case class RecommendationJson(sender: UserJson, receiver: UserJson, destination: Destination, note: Option[String])

object Recommendation extends SlickDbObject[Recommendation, (Int, Int, Int, Int, Option[String]), Tables.Recommendations] {
  val table = Tables.recommendations

  def reify(tuple: (Int, Int, Int, Int, Option[String])) =
    (apply _).tupled(tuple)

  def classToTuple(a: Recommendation) =
    unapply(a).get

  def safeSaveManyForOneSender(shares: Seq[Recommendation], senderId: Int) = {
    recommendationsSharedBySenderIdRows(senderId).flatMap(alreadySaved => {
      createMany(shares).flatMap(newlySaved => {
        val recipientDeviceIds = DeviceToken.getByUserIds(newlySaved.map(_.receiverUserId))
        val senders = Await.result(User.byIds(newlySaved.map(_.senderUserId)), Duration.Inf).map(a => a.id -> a.username).toMap
        val destinations = Await.result(Destination.byIds(newlySaved.map(_.destinationId)), Duration.Inf).map(a => a.id -> a.name).toMap
        newlySaved.foreach(saved => {
          val sendername = senders(saved.senderUserId)
          val receiverDeviceToken = recipientDeviceIds(saved.receiverUserId)
          val adventure = destinations(saved.destinationId)
/*          if(receiverDeviceToken.isDefined) {
            System.out.println("Making push notification");
            PushNotificationManager.makePushNotification(sendername + " shared an adventure, " + adventure + ", with you", receiverDeviceToken.get)
          } else {
            System.out.println("Not pushing");
          }*/
        })

        val alreadySavedByTriple = alreadySaved.map(a => (a.senderUserId, a.receiverUserId, a.destinationId) -> a).toMap
        val toDelete = newlySaved.flatMap(a => alreadySavedByTriple.get(a.senderUserId, a.receiverUserId, a.destinationId))
        deleteMany(toDelete.map(_.id)).map(_ => newlySaved)

      })
    })
  }

  def recommendationsBySenderId(senderUserId: Int) = {
    db.run(
      (for {
        recommendations <- table.filter(_.senderUserId === senderUserId)
        destinations <- Destination.table if destinations.id === recommendations.destinationId
        receivers <- User.table if receivers.id === recommendations.receiverUserId
        senders <- User.table if senders.id === recommendations.senderUserId
      } yield (destinations, receivers, senders, recommendations)).result).map(_.map(a => {
      RecommendationJson(
        sender = User.reifyJson(a._3),
        receiver = User.reifyJson(a._2),
        destination = Destination.reify(a._1),
        note = a._4._5
      )
    }))
  }

  def recommendationsByReceiverId(receiverUserId: Int) = {
    db.run(
      (for {
        userConnections <- UserConnection.table.filter(_.senderUserId === receiverUserId)
        recommendations <- table.filter(_.receiverUserId === receiverUserId) if recommendations.senderUserId === userConnections.receiverUserId
        destinations <- Destination.table if destinations.id === recommendations.destinationId
        receivers <- User.table if receivers.id === recommendations.receiverUserId
        senders <- User.table if senders.id === recommendations.senderUserId
      } yield (destinations, receivers, senders, recommendations)).result).map(_.map(a => {
      RecommendationJson(
        sender = User.reifyJson(a._3),
        receiver = User.reifyJson(a._2),
        destination = Destination.reify(a._1),
        note = a._4._5
      )
    }))
  }

  def recommendationsByReceiverIdAndDestination(receiverUserId: Int, destinationId: Int) = {
    db.run(
      (for {
        userConnections <- UserConnection.table.filter(_.senderUserId === receiverUserId)
        recommendations <- table.filter(a => a.receiverUserId === receiverUserId && a.destinationId === destinationId) if recommendations.senderUserId === userConnections.receiverUserId
        destinations <- Destination.table if destinations.id === recommendations.destinationId
        receivers <- User.table if receivers.id === recommendations.receiverUserId
        senders <- User.table if senders.id === recommendations.senderUserId
      } yield (destinations, receivers, senders, recommendations)).result).map(_.map(a => {
      RecommendationJson(
        sender = User.reifyJson(a._3),
        receiver = User.reifyJson(a._2),
        destination = Destination.reify(a._1),
        note = a._4._5
      )
    }))
  }

  def recommendationsSharedBySenderIdRows(senderUserId: Int) = {
    db.run(table.filter(_.senderUserId === senderUserId).result).map(_.map(reify))
  }
}
