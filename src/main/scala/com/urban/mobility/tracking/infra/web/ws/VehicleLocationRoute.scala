package com.urban.mobility.tracking.infra.web.ws

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Sink, Source}
import com.urban.mobility.shared.domain.model.Vehicle
import com.urban.mobility.topology.domain.model.TopologicalLocation
import com.urban.mobility.tracking.application.TrackingService
import spray.json._

object VehicleLocationApi {
  val API = "mobility"
  val API_VERSION_V1 = "v1"
  val API_VERSION_V2 = "v2"
  val VEHICLE_COLLECTION = "vehicles"
  val ALL_LOCATIONS_RESOURCE = "locations"
}

private class VehicleLocationRoute[T](val version: String, val source: Source[Map[Vehicle.ID, TopologicalLocation], T])
  extends VehicleLocationJson {

  import VehicleLocationApi._

  val route: Route =
    pathPrefix(API / version / VEHICLE_COLLECTION / ALL_LOCATIONS_RESOURCE) {
      extractUpgradeToWebSocket { upgrade =>
        complete(upgrade.handleMessagesWithSinkSource(Sink.ignore, outputSource))
      }
    }

  private val outputSource = source
    .map(_.toJson.sortedPrint)
    .map(_.replaceAll("\\s", ""))
    .map(text => TextMessage.Strict(text))
}

object VehicleLocationRoute {

  import VehicleLocationApi._

  def pullBased(service: TrackingService)(implicit system: ActorSystem[Nothing]): Route =
    new VehicleLocationRoute(API_VERSION_V1, PullBasedVehicleLocationSource(() => service.getVehicles)).route

  def pushBased(service: TrackingService)(implicit system: ActorSystem[Nothing]): Route =
    new VehicleLocationRoute(API_VERSION_V2, PushBasedVehicleLocationSource(service.locateVehicle)).route
}
