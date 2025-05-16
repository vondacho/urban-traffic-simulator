package com.urban.mobility.travelling.infra.web.rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.urban.mobility.travelling.application.TravelService

import scala.concurrent.ExecutionContext

object ArrivalTimeApi {
  val API = "mobility"
  val API_VERSION = "v1"
  val VEHICLE_COLLECTION = "vehicles"
  val ARRIVAL_TIMES_RESOURCE = "arrival-times"
}

class ArrivalTimeRoute(val service: TravelService)(implicit val executor: ExecutionContext)
  extends TravelStatisticsJson {

  import ArrivalTimeApi._

  val route: Route =
    pathPrefix(API / API_VERSION / VEHICLE_COLLECTION) {
      path(Segment / ARRIVAL_TIMES_RESOURCE) { vehicleID =>
        get(
          complete(service.estimatedArrivalTimeToAnyReachableNode(vehicleID)
            .map(_.map { case (node, eat) => (node.toString, eat) }))
        )
      }
    }
}

object ArrivalTimeRoute {
  def apply(service: TravelService)(implicit executor: ExecutionContext): Route =
    new ArrivalTimeRoute(service).route
}





