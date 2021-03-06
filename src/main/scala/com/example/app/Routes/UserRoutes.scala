package com.example.app.Routes

import com.example.app.models._
import com.example.app.{AuthenticationSupport, SessionTokenStrategy, SlickRoutes}
import org.scalatra.Ok

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

trait UserRoutes extends SlickRoutes with AuthenticationSupport{

  post("/users/create") {
    contentType = formats("json")
    //val user = parsedBody.extract[UserCreate]

    val username = request.header(SessionTokenStrategy.Username)
    val email = request.header(SessionTokenStrategy.Email)
    val password = request.header(SessionTokenStrategy.Password)

    val user = UserCreate(username.get, email.get, password.get)

    val created = User.createNewUser(user)

    created.map(_.toJson)
  }

  post("/users/search") {
    contentType = formats("json")
    authenticate()

    val userId = user.id

    val query = {params("query")}

    User.searchUserName(query).map(_.filterNot(_.id == userId))
  }

  post("/users/validate/username") {
    contentType = formats("json")

    val username = {params("username")}

    User.validUsername(username) && Await.result(User.uniqueUsername(username), Duration.Inf)
  }

  post("/users/validate/email") {
    contentType = formats("json")

    val email = {params("email")}

    User.validEmail(email) && Await.result(User.uniqueEmail(email), Duration.Inf)
  }

  post("/users/connections/create") {
    contentType = formats("json")
    authenticate()

    val connectionRequest = parsedBody.extract[ConnectionRequestJson]
    val connection = connectionRequest.newConnection(user.id)

    UserConnection.safeSave(connection, user)
  }

  post("/users/connections/delete") {
    contentType = formats("json")
    authenticate()

    val rejectionRequest = parsedBody.extract[ConnectionDeleteJson]

    UserConnection.removeBySenderReceiverPair(user.id, rejectionRequest.removeUserId).map(_ => "200")
  }

  get("/users/connections/added") {
    contentType = formats("json")
    authenticate()

    UserConnection.getReceiversBySenderId(user.id)
  }

  get("/users/connections/awaiting") {
    contentType = formats("json")
    authenticate()

    UserConnection.awaitingConnectionsFor(user.id)
  }

  get("/users/connections/suggested") {
    contentType = formats("json")
    authenticate()

    UserConnection.allSuggestedConnectionsFor(user.id)
  }

  get("/users") {
    User.getAll.map(_.map(_.toJson))
  }

  post("/users/tokens"){
    contentType = formats("json")
    authenticate()

    val rawToken = {params("device_token")}

    val deviceToken = DeviceToken(userId = user.id, deviceToken = Some(rawToken))
    DeviceToken.save(deviceToken)
  }

  get("/users/tokens") {
    contentType = formats("json")

    DeviceToken.getAll
  }

}