package com.example.app

import slick.driver.PostgresDriver.api._


object Tables {

  class Users(tag: Tag) extends Table[(Int, String, String, String)](tag, "USER_ACCOUNTS") with HasIdColumn[Int] {
    def id = column[Int]("USER_ACCOUNT_ID", O.PrimaryKey, O.AutoInc)
    def username = column[String]("USERNAME")
    def email = column[String]("EMAIL")
    def hashedPassword = column[String]("HASHED_PASSWORD")

    def * = (id, username, email, hashedPassword)
  }

  class DeviceTokens(tag: Tag) extends Table[(Int, Int, Option[String])](tag, "DEVICE_TOKENS") with HasIdColumn[Int] {
    def id = column[Int]("DEVICE_TOKEN_ID", O.PrimaryKey, O.AutoInc)
    def userId = column[Int]("USER_ID")
    def deviceToken = column[Option[String]]("DEVICE_TOKEN")

    def * = (id, userId, deviceToken)

    def user = foreignKey("DEVICE_TOKENS_TO_USER_FK", userId, users)(_.id)
  }

  class UserSessions(tag: Tag) extends Table[(Int, Int, String)](tag, "USER_SESSIONS") with HasIdColumn[Int] {
    def id = column[Int]("USER_SESSION_ID", O.PrimaryKey, O.AutoInc)
    def userId = column[Int]("USER_ID")
    def hashString = column[String]("HASH_STRING")

    def * = (id, userId, hashString)

    def user = foreignKey("USER_SESSIONS_TO_USER_FK", userId, users)(_.id)
  }

  class UserConnections(tag: Tag) extends Table[(Int, Int, Int)](tag, "USER_CONNECTIONS") with HasIdColumn[Int] {
    def id = column[Int]("USER_CONNECTION_ID", O.PrimaryKey, O.AutoInc)
    def senderUserId = column[Int]("SENDER_USER_ID")
    def receiverUserId = column[Int]("RECEIVER_USER_ID")

    def * = (id, senderUserId, receiverUserId)

    def sender = foreignKey("USER_CONNECTIONS_SENDER_TO_USERS_FK", senderUserId, users)(_.id)
    def receiver = foreignKey("USER_CONNECTIONS_RECEIVER_TO_USERS_FK", receiverUserId, users)(_.id)
  }

  class Destinations(tag: Tag) extends Table[(Int, String, String, Double, Double)](tag, "DESTINATIONS") with HasIdColumn[Int] {
    def id = column[Int]("DESTINATION_ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("DESTINATION_NAME")
    def address = column[String]("ADDRESS")
    def latitude = column[Double]("LATITUDE")
    def longitude = column[Double]("LONGITUDE")

    def * = (id, name, address, latitude, longitude)
  }

  class Recommendations(tag: Tag) extends Table[(Int, Int, Int, Int, Option[String])](tag, "RECOMMENDATIONS") with HasIdColumn[Int] {
    def id = column[Int]("RECOMMENDATION_ID", O.PrimaryKey, O.AutoInc)
    def senderUserId = column[Int]("SENDER_USER_ID")
    def receiverUserId = column[Int]("RECEIVER_USER_ID")
    def destinationId = column[Int]("DESTINATION_ID")
    def note = column[Option[String]]("NOTE")

    def * = (id, senderUserId, receiverUserId, destinationId, note)

    def sender = foreignKey("RECOMMENDATIONS_SENDER_TO_USER_FK", senderUserId, users)(_.id)
    def receiver = foreignKey("RECOMMENDATIONS_RECEIVER_TO_USER_FK", receiverUserId, users)(_.id)
    def destination = foreignKey("RECOMMENDATIONS_TO_DESTINATION_FK", destinationId, destinations)(_.id)
  }

  class Reviews(tag: Tag) extends Table[(Int, Int, Int, Boolean, Option[String])](tag, "REVIEWS") with HasIdColumn[Int] {
    def id = column[Int]("REVIEW_ID", O.PrimaryKey, O.AutoInc)
    def userId = column[Int]("USER_ID")
    def destinationId = column[Int]("DESTINATION_ID")
    def positiveRating = column[Boolean]("POSITIVE_RATING")
    def note = column[Option[String]]("NOTE")

    def * = (id, userId, destinationId, positiveRating, note)

    def user = foreignKey("REVIEWS_TO_USER_FK", userId, users)(_.id)
    def destination = foreignKey("REVIEWS_TO_DESTINATION_FK", destinationId, destinations)(_.id)
  }

  val users = TableQuery[Users]
  val deviceTokens = TableQuery[DeviceTokens]
  val userSessions = TableQuery[UserSessions]

  val userConnections = TableQuery[UserConnections]

  val destinations = TableQuery[Destinations]

  val recommendations = TableQuery[Recommendations]
  val reviews = TableQuery[Reviews]


  val schemas = (users.schema ++ userSessions.schema ++ deviceTokens.schema ++ userConnections.schema ++ destinations.schema ++ recommendations.schema ++ reviews.schema)


  // DBIO Action which creates the schema
  val createSchemaAction = schemas.create

  // DBIO Action which drops the schema
  val dropSchemaAction = schemas.drop

}

trait HasIdColumn[A]{
  def id: Rep[A]
}
