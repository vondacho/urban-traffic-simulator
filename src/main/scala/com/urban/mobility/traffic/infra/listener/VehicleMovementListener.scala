package com.urban.mobility.traffic.infra.listener

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.urban.mobility.shared.domain.model.VehiclePositionChanged

object VehicleMovementListener {

  def apply(handlers: List[VehiclePositionChanged => Unit])  : Behavior[VehiclePositionChanged] =
    Behaviors.receiveMessage {
      event =>
        handlers.foreach(_ (event))
        Behaviors.same
    }
}
