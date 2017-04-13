package com.example.app.Routes

import com.example.app.models.{Recommendation, RecommendationJsonRequest, Review, ReviewJsonRequest}
import com.example.app.{AuthenticationSupport, SlickRoutes}

/**
  * Created by matt on 3/29/17.
  */
trait RecommendationRoutes extends SlickRoutes with AuthenticationSupport {

  post("/destinations/share") {
    contentType = formats("json")
    authenticate()

    val recommendationRequests = parsedBody.extract[Seq[RecommendationJsonRequest]]
    val recommendations = recommendationRequests.map(_.newRecommendation(user.id))
    Recommendation.safeSaveManyForOneSender(recommendations, user.id)
  }

  get("/destinations/shared") {
    contentType = formats("json")
    authenticate()

    Recommendation.recommendationsBySenderId(user.id)
  }

  get("/destinations/received") {
    contentType = formats("json")
    authenticate()

    Recommendation.recommendationsByReceiverId(user.id)
  }

  post("/destinations/review/save") {
    contentType = formats("json")
    authenticate()

    val review = parsedBody.extract[ReviewJsonRequest]

    val u = user

    Review.saveReviewByUserForDestination(u.id, review).map(_.toJson(u.toJson))
  }

  get("/recommendations") {
    contentType = formats("json")

    Recommendation.getAll
  }

  get("/destinations/reviews") {
    contentType = formats("json")

    Review.getAll
  }
}
