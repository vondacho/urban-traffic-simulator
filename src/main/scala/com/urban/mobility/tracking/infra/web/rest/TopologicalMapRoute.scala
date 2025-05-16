package com.urban.mobility.tracking.infra.web.rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.urban.mobility.tracking.application.TrackingService

import scala.concurrent.ExecutionContext

object TopologicalMapApi {
  val API = "mobility"
  val API_VERSION = "v1"
  val MAP_RESOURCE = "topological-map"
  val ALL_VEHICLE_LOCATION_RESOURCE = "vehicles"
}

class TopologicalMapRoute(val service: TrackingService)(implicit val executor: ExecutionContext)
  extends TopologicalMapJson {

  import TopologicalMapApi._

  val route: Route =
    pathPrefix(API / API_VERSION / MAP_RESOURCE) {
      concat(
        pathEnd {
          get(complete(service.getMap.map(TopologicalMapAdapter(_))))
        },
        path(ALL_VEHICLE_LOCATION_RESOURCE) {
          get(complete(service.getMap.map(TopologicalMapAdapter.vehicles)))
        }
      )
    }
}

object TopologicalMapRoute {
  def apply(service: TrackingService)(implicit executor: ExecutionContext): Route =
    new TopologicalMapRoute(service).route
}
