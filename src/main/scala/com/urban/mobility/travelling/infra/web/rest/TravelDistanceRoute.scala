package com.urban.mobility.travelling.infra.web.rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.urban.mobility.travelling.application.TravelService

import scala.concurrent.ExecutionContext

object TravelDistanceApi {
  val API = "mobility"
  val API_VERSION = "v1"
  val VEHICLE_COLLECTION = "vehicles"
  val TRAVEL_DISTANCES_RESOURCE = "travel-distance"
}

class TravelDistanceRoute(val service: TravelService)(implicit val executor: ExecutionContext)
  extends TravelStatisticsJson {

  import TravelDistanceApi._

  val route: Route =
    pathPrefix(API / API_VERSION / VEHICLE_COLLECTION / TRAVEL_DISTANCES_RESOURCE) {
      concat(
        pathEnd {
          get {
            complete(service.getDistances)
          }
        }
      )
    }
}

object TravelDistanceRoute {
  def apply(service: TravelService)(implicit executor: ExecutionContext): Route =
    new TravelDistanceRoute(service).route
}


