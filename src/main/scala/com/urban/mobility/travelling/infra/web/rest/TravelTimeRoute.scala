package com.urban.mobility.travelling.infra.web.rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.urban.mobility.travelling.application.TravelService

import scala.concurrent.ExecutionContext

object TravelTimeApi {
  val API = "mobility"
  val API_VERSION = "v1"
  val SEGMENT_COLLECTION = "segments"
  val AVERAGE_TRAVEL_TIME_RESOURCE = "travel-time"
}

class TravelTimeRoute(val service: TravelService)(implicit val executor: ExecutionContext)
  extends TravelStatisticsJson {

  import TravelTimeApi._

  val route: Route =
    pathPrefix(API / API_VERSION / SEGMENT_COLLECTION / AVERAGE_TRAVEL_TIME_RESOURCE) {
      get {
        complete(service.getAverageTimes.map(response => response.map {
          case (edge, value) => edge.toString() -> value
        }))
      }
    }
}

object TravelTimeRoute {
  def apply(service: TravelService)(implicit executor: ExecutionContext): Route =
    new TravelTimeRoute(service).route
}




