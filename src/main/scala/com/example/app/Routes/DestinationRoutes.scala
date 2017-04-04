package com.example.app.Routes

import com.example.app.models.{Destination, DestinationForUser}
import com.example.app.{AuthenticationSupport, SlickRoutes}

/**
  * Created by matt on 3/31/17.
  */
trait DestinationRoutes extends SlickRoutes with AuthenticationSupport {

  post("/destinations/retrieve") {
    contentType = formats("json")
    authenticate()

    val destinationLookup = parsedBody.extract[Destination]

    DestinationForUser.getDestinationLookup(user.id, destinationLookup)

    //Destination.getDestinationLookup(destinationLookup)
  }

  get("/destinations/personalized") {
    contentType = formats("json")
    authenticate()

    DestinationForUser.getDestinationsForUser(user.id)
  }

  get("/destinations/one/personalized") {
    contentType = formats("json")
    authenticate()
  }

  get("/destinations/all") {
    contentType = formats("json")

    Destination.getAll
  }
}
