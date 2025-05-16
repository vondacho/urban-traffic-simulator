package com.urban.mobility.traffic.infra.handler

import com.urban.mobility.shared.domain.model.VehiclePositionChanged

object VehicleMovementLogger {

  def moveVehicle: VehiclePositionChanged => Unit = {
    case VehiclePositionChanged(vehicleID, absolute, when) => println(s"${(vehicleID, absolute, when)}")
  }
}
