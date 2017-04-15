package com.example.app.models


import com.example.app.AppGlobals
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

case class DestinationForUser(
  destination: Destination,
  inboundRecommendations: Seq[RecommendationJson],
  reviews: Seq[ReviewJson],
  ownReview: Option[ReviewJson],
  thanksSent: Seq[Thank],
  thanksReceived: Seq[Thank])

object DestinationForUser {

  def getDestinationsForUser(userId: Int) = {
    (
      for {
        suggestions <- Recommendation.recommendationsByReceiverId(userId)
        reviews <- Review.getReviewsForUser(userId)
        ownReviews <- Review.getReviewsByUser(userId)
        users <- User.byIds(reviews.map(_.userId) :+ userId).map(_.map(_.toJson))
        destinations <- Destination.byIds(reviews.map(_.destinationId) ++ ownReviews.map(_.destinationId) ++ suggestions.map(_.destination.id))
        thanks <- Thank.thanksForUserAndDestinations(userId, destinations.map(_.id))
      } yield (suggestions, reviews, ownReviews, users, destinations, thanks)).map{ case (sugs, revs, ownRevs, us, ds, ts) => {
      val allDestinations = ds.distinct
      val userById = us.map(u => u.id -> u).toMap
      val suggestionsByDestinationId = sugs.groupBy(_.destination.id)
      val reviewsByDestinationId = revs.groupBy(_.destinationId).mapValues(_.map(a => a.toJson(userById(a.userId))))
      val myReviewsByDestinationId = ownRevs.map(o => o.destinationId -> o.toJson(userById(o.userId))).toMap
      val (thanksSent, thanksReceived) = ts.partition(_.senderUserId == userId)
      val thanksReceivedByDestination = thanksReceived.groupBy(_.destinationId)
      val thanksSentByDestination = thanksSent.groupBy(_.destinationId)
      allDestinations.map(d => {
        DestinationForUser(
          d,
          suggestionsByDestinationId.get(d.id).getOrElse(Nil),
          reviewsByDestinationId.get(d.id).getOrElse(Nil),
          myReviewsByDestinationId.get(d.id),
          thanksSentByDestination.get(d.id).getOrElse(Nil),
          thanksReceivedByDestination.get(d.id).getOrElse(Nil)
        )
      })
    }}

  }

  def getDestinationsReviewedByUserForUser(reviewerUserId: Int, userId: Int) = {
    (
      for {
        suggestions <- Recommendation.recommendationsByReceiverId(userId)
        reviews <- Review.getReviewsForUser(userId)
        ownReviews <- Review.getReviewsByUser(userId)
        users <- User.byIds(reviews.map(_.userId) :+ userId).map(_.map(_.toJson))
        destinations <- Destination.byIds(reviews.filter(_.userId == reviewerUserId).map(_.destinationId))
        thanks <- Thank.thanksForUserAndDestinations(userId, destinations.map(_.id))
      } yield (suggestions, reviews, ownReviews, users, destinations, thanks)).map{ case (sugs, revs, ownRevs, us, ds, ts) => {
      val allDestinations = ds.distinct
      val userById = us.map(u => u.id -> u).toMap
      val suggestionsByDestinationId = sugs.groupBy(_.destination.id)
      val reviewsByDestinationId = revs.groupBy(_.destinationId).mapValues(_.map(a => a.toJson(userById(a.userId))))
      val myReviewsByDestinationId = ownRevs.map(o => o.destinationId -> o.toJson(userById(o.userId))).toMap
      val (thanksSent, thanksReceived) = ts.partition(_.senderUserId == userId)
      val thanksReceivedByDestination = thanksReceived.groupBy(_.destinationId)
      val thanksSentByDestination = thanksSent.groupBy(_.destinationId)
      allDestinations.map(d => {
        DestinationForUser(
          d,
          suggestionsByDestinationId.get(d.id).getOrElse(Nil),
          reviewsByDestinationId.get(d.id).getOrElse(Nil),
          myReviewsByDestinationId.get(d.id),
          thanksSentByDestination.get(d.id).getOrElse(Nil),
          thanksReceivedByDestination.get(d.id).getOrElse(Nil)
        )
      })
    }}

  }

  def getDestinationLookup(userId: Int, lookup: Destination) = {
    val alreadyExists = Await.result(Destination.byDestinationLookup(lookup), Duration.Inf)
    alreadyExists.map(d => getOneDestinationForUser(userId, d.id)).getOrElse{
      val newone = Destination.create(lookup)
      newone.map(nd => DestinationForUser(nd, Nil, Nil, None, Nil, Nil))
    }
  }

  def getOneDestinationForUser(userId: Int, destinationId: Int) = {
    val db = AppGlobals.db()

    (
      for {
        suggestions <- Recommendation.recommendationsByReceiverIdAndDestination(userId, destinationId)
        reviews <- Review.getReviewsForUserForDestination(userId, destinationId)
        ownReviews <- Review.getReviewByUserForDestination(userId, destinationId)
        users <- User.byIds(reviews.map(_.userId) :+ userId).map(_.map(_.toJson))
        destination <- Destination.byId(destinationId)
        thanks <- Thank.thanksForUserAndDestinations(userId, Seq(destination.id))
      } yield (suggestions, reviews, ownReviews, users, destination, thanks)).map{ case (sugs, revs, ownRevs, us, d, ts) => {
      val userById = us.map(u => u.id -> u).toMap
      val (thanksSent, thanksReceived) = ts.partition(_.senderUserId == userId)
      DestinationForUser(
        d,
        sugs,
        revs.map(a => a.toJson(userById(a.userId))),
        ownRevs.map(a => a.toJson(userById(a.userId))),
        thanksSent,
        thanksReceived
      )
    }}
  }
}
