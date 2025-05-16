package com.urban.mobility.tracking.application

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.urban.mobility.shared.domain.model.{Vehicle, VehiclePositionChanged}
import com.urban.mobility.topology.domain.model.{NetworkMap, TopologicalLocation, TopologicalMap}
import com.urban.mobility.tracking.domain.TopologicalMapEntity._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

case class TrackingService(registry: ActorRef[Command], network: NetworkMap)
                          (implicit val system: ActorSystem[Nothing],
                           implicit val executor: ExecutionContext) {

  private implicit val askTimeout: Timeout = new Timeout(5.seconds)

  def getMap: Future[Option[TopologicalMap]] = (registry ? GetMap)
    .map({ case CurrentMap(map) => map })

  def getVehicles: Future[Map[Vehicle.ID, TopologicalLocation]] = (registry ? GetVehicles)
    .map({ case CurrentVehicles(vehicles) => vehicles.getOrElse(Map()) })

  def moveVehicle: VehiclePositionChanged => Unit = {
    case VehiclePositionChanged(vehicleID, absolute, _) =>
      network.toLocation(absolute) match {
        case Some(location) => registry ! MoveVehicle(vehicleID, location)
        case _ =>
      }
  }

  def locateVehicle: VehiclePositionChanged => Option[(Vehicle.ID, TopologicalLocation)] = {
    case VehiclePositionChanged(vehicleID, absolute, _) =>
      network.toLocation(absolute).flatMap(TopologicalLocation.from).map((vehicleID, _))
  }
}
