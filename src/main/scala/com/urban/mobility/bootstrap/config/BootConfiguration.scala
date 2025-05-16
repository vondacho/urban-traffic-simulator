package com.urban.mobility.bootstrap.config

import akka.actor.typed.eventstream.EventStream
import akka.actor.typed.scaladsl.ActorContext
import com.urban.mobility.traffic.infra.listener
import com.urban.mobility.traffic.application.service.VehiclesService
import com.urban.mobility.topology.infra.config.TopologyConfiguration
import com.urban.mobility.tracking.application.TrackingService
import com.urban.mobility.tracking.infra.config.TrackingConfiguration
import com.urban.mobility.travelling.application.TravelService
import com.urban.mobility.travelling.infra.config.TravelConfiguration

import scala.concurrent.ExecutionContext

case class BootConfiguration(trackingService: TrackingService, travelService: TravelService) {
}

object BootConfiguration {
  def apply()(implicit context: ActorContext[Nothing], executor: ExecutionContext): BootConfiguration = {

    val TopologyConfiguration(network) = TopologyConfiguration("/lc-track0-21781.geojson")
    val TrackingConfiguration(trackingService) = TrackingConfiguration(network)
    val TravelConfiguration(travelService) = TravelConfiguration(network)

    val vehicleMovementListener = context.spawn(listener.VehicleMovementListener(List(
      trackingService.moveVehicle,
      travelService.moveVehicle/*,
      VehicleMovementLogger.moveVehicle*/)
    ), "vehicleMovementListener")

    new VehiclesService(network, 3)(context.system)

    context.system.eventStream ! EventStream.Subscribe(vehicleMovementListener)

    BootConfiguration(trackingService, travelService)
  }
}
