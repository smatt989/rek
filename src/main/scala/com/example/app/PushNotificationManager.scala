package com.example.app

import java.io.File
import java.util.concurrent.ExecutionException

import com.relayrides.pushy.apns._
import com.relayrides.pushy.apns.util.{ApnsPayloadBuilder, SimpleApnsPushNotification, TokenUtil}
import java.util.concurrent.{Future => JFuture}

import com.example.app.models.DeviceToken

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise, Future => SFuture}
import scala.util.Try

object PushNotificationManager {

  val apnsClient = new ApnsClientBuilder().build()
  val topic = "com.rek.app"
  val teamId = System.getenv("REKKI_TEAM_ID")
  val keyId = System.getenv("APNS_KEY_ID")

  apnsClient.registerSigningKey(Thread.currentThread.getContextClassLoader.getResourceAsStream("src/main/resources/APNsAuthKey_A3Q9C7HYSV.p8"),
    teamId, keyId, topic)

  //val developerGateway = "gateway.sandbox.push.apple.com"
  //val productionGateway = "gateway.push.apple.com"
  connect()

  val t = new java.util.Timer()
  val task = new java.util.TimerTask {
    def run() = resetConnection()
  }
  t.schedule(task, 1000 * 60 * 30, 1000 * 60 * 30)

  def connect(): Unit = {
    val jfuture: JFuture[Void] = apnsClient.connect(ApnsClient.PRODUCTION_APNS_HOST)
    val promise = Promise[Void]()
    new Thread(new Runnable { def run() { promise.complete(Try{ jfuture.get }) }}).start
    val future = promise.future

    System.out.println("Establishing connection...")

    Await.result(future, Duration.Inf)
  }

  def resetConnection(): Unit = {
    System.out.println("resetting connection...")
    apnsClient.disconnect()
    connect()
  }

  def pushNotificationsFor(message: String, userIds: Seq[Int]) = {
    val tokens = DeviceToken.getByUserIds(userIds)
    userIds.map(user => {
      tokens(user).map(token => makePushNotification(message, token))
    })
  }

  def makePushNotification(message: String, deviceToken: String) = {

    val payloadBuilder = new ApnsPayloadBuilder()
    payloadBuilder.setAlertBody(message)

    val payload = payloadBuilder.buildWithDefaultMaximumLength()

    val token = TokenUtil.sanitizeTokenString(deviceToken)

    val pushNotification = new SimpleApnsPushNotification(token, topic, payload)
    System.out.println("Sending notification...")
    val sendNotificationFuture = apnsClient.sendNotification(pushNotification)

    try {
      val pushNotificationResponse =
        sendNotificationFuture.get();

      System.out.println("Push notification received...")
      if (pushNotificationResponse.isAccepted()) {
        System.out.println("Push notification accepted by APNs gateway.")
      } else {
        System.out.println("Notification rejected by the APNs gateway: " +
          pushNotificationResponse.getRejectionReason())

        if (pushNotificationResponse.getTokenInvalidationTimestamp() != null) {
          System.out.println("\t…and the token is invalid as of " +
            pushNotificationResponse.getTokenInvalidationTimestamp())
        }
      }
    } catch {case e: ExecutionException =>
      System.err.println("Failed to send push notification.")
      e.printStackTrace()

      if (e.getCause().isInstanceOf[ClientNotConnectedException]) {
        System.out.println("Waiting for client to reconnect…")
        apnsClient.getReconnectionFuture().await()
        System.out.println("Reconnected.")
      }
    }

/*    val jfutureDisconnect: JFuture[Void] = apnsClient.disconnect()
    val promiseDisconnect = Promise[Void]()
    new Thread(new Runnable { def run() { promiseDisconnect.complete(Try{ jfutureDisconnect.get }) }}).start
    val futureDisconnect = promiseDisconnect.future

    System.out.println("Disconnecting...")

    Await.result(futureDisconnect, Duration.Inf)
    System.out.println("Done.")*/

  }
}
