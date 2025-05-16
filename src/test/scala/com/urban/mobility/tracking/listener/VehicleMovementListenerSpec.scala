package com.urban.mobility.tracking.listener

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, ScalaTestWithActorTestKit, TestInbox}
import com.urban.mobility.traffic.infra.listener.VehicleMovementListener
import com.urban.mobility.shared.domain.model.VehiclePositionChanged
import com.urban.mobility.tracking.domain.TopologicalMapEntity.{Command, MoveVehicle}
import com.urban.mobility.topology.domain.model.NetworkLocation
import com.vividsolutions.jts.geom.Coordinate
import org.scalatest.flatspec.AnyFlatSpecLike

class VehicleMovementListenerSpec extends ScalaTestWithActorTestKit with AnyFlatSpecLike {

  "A VehicleMovement listener" must "handle a VehiclePositionChanged message" in {
    // given
    val toLocation: Coordinate => NetworkLocation =
      coordinate => NetworkLocation((coordinate.x.toInt, coordinate.y.toInt), 1.0)

    val inbox = TestInbox[Command]()
    val testKit = BehaviorTestKit(VehicleMovementListener(List(
      e => inbox.ref ! MoveVehicle("v1", toLocation(e.absolute)))))

    // when
    testKit.run(VehiclePositionChanged("v1", new Coordinate(5.0, 0.0)))
    // then
    inbox.expectMessage(MoveVehicle("v1", NetworkLocation((5, 0), 1.0)))
  }
}
