package com.urban.mobility.tracking.infra.web.ws

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import com.urban.mobility.shared.domain.model.Vehicle
import com.urban.mobility.topology.domain.model.TopologicalLocation

import scala.concurrent.Future
import scala.concurrent.duration._

object PullBasedVehicleLocationSource {

  def apply(source: () => Future[Map[Vehicle.ID, TopologicalLocation]]):
  Source[Map[String, TopologicalLocation], Cancellable] =
    Source
      .tick(0.seconds, 1.seconds, 1)
      .mapAsync(1)(_ => source())
}
