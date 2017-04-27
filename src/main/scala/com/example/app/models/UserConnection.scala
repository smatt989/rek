package com.example.app.models

import com.example.app.{HasIntId, PushNotificationManager, SlickDbObject, Tables}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

case class UserConnection(id: Int = 0, senderUserId: Int, receiverUserId: Int) extends HasIntId[UserConnection]{

  def updateId(id: Int) =
    this.copy(id = id)
}

case class ConnectionRequestJson(addUserId: Int) {
  def newConnection(senderUserId: Int) = {
    UserConnection(
      senderUserId = senderUserId,
      receiverUserId = addUserId
    )
  }
}

case class ConnectionDeleteJson(removeUserId: Int)

object UserConnection extends SlickDbObject[UserConnection, (Int, Int, Int), Tables.UserConnections]{

  lazy val table = Tables.userConnections

  def reify(tuple: (Int, Int, Int)) =
    UserConnection(tuple._1, tuple._2, tuple._3)

  def classToTuple(a: UserConnection) =
    (a.id, a.senderUserId, a.receiverUserId)

  def safeSave(connection: UserConnection, sender: User) = {
    findConnection(connection.senderUserId, connection.receiverUserId).flatMap(optionalConnection => {
      if (optionalConnection.isEmpty) {
        create(connection).map(a => {
          PushNotificationManager.pushNotificationsFor(sender.username +" has started following you", Seq(a.receiverUserId))
        })
      } else {
        Future.apply(optionalConnection.get)
      }
    })
  }

  def awaitingConnectionsFor(userId: Int) = {
    val sent = getReceiversBySenderId(userId)
    val received = getSendersByReceiverId(userId)

    for {
      s <- sent
      r <- received
    } yield (r diff s)
  }

  def suggestedConnectionsFor(userId: Int) = {
    db.run(
      (for {
        myUserConnections <- table.filter(_.senderUserId === userId)
        theirUserConnections <- table if myUserConnections.receiverUserId === theirUserConnections.senderUserId
        users <- User.table if users.id === theirUserConnections.receiverUserId
      } yield (users)

        ).result
    ).map(_.map(User.reify).map(_.toJson))
  }

  def allSuggestedConnectionsFor(userId: Int) = {
    val sent = Await.result(getReceiversBySenderId(userId), Duration.Inf)

    val received = Await.result(getSendersByReceiverId(userId), Duration.Inf)

    val suggestions = Await.result(db.run(
      (for {
        myUserConnections <- table.filter(_.senderUserId === userId)
        theirUserConnections <- table if myUserConnections.receiverUserId === theirUserConnections.senderUserId
        users <- User.table if users.id === theirUserConnections.receiverUserId
      } yield (users)

        ).result
    ).map(_.map(User.reify).map(_.toJson)), Duration.Inf)

    (suggestions ++ received).distinct diff sent

  }

  def findConnection(senderUserId: Int, receiverUserId: Int) =
    db.run(table.filter(a => a.senderUserId === senderUserId && a.receiverUserId === receiverUserId).result).map(_.map(reify).headOption)

  def getBySenderId(senderId: Int) =
    db.run(table.filter(_.senderUserId === senderId).result).map(_.map(reify))

  def getReceiversBySenderId(senderId: Int) =
    db.run(
      (for {
        (cs, us) <- table.filter(_.senderUserId === senderId) join Tables.users on (_.receiverUserId === _.id)
      } yield (us)).result).map(_.map(User.reifyJson)
    )

  def getByReceiverId(receiverId: Int) =
    db.run(table.filter(_.receiverUserId === receiverId).result).map(_.map(reify))

  def getSendersByReceiverId(receiverId: Int) =
    db.run(
      (for {
        (cs, us) <- table.filter(_.receiverUserId === receiverId) join Tables.users on (_.senderUserId === _.id)
      } yield (us)).result).map(_.map(User.reifyJson))

  def removeBySenderReceiverPair(senderUserId: Int, receiverUserId: Int) =
    db.run(table.filter(a => a.senderUserId === senderUserId && a.receiverUserId === receiverUserId).delete)
}