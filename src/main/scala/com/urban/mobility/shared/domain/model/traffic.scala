package com.urban.mobility.shared.domain.model

import java.time.Instant

import com.vividsolutions.jts.geom.Coordinate

object Vehicle {
  type ID = String
}

trait VehicleEvent {
  def vehicleID: Vehicle.ID
  def timestamp: Instant
}

case class VehiclePositionChanged(vehicleID: Vehicle.ID,
                                  absolute: Coordinate,
                                  timestamp: Instant = Instant.now()) extends VehicleEvent
