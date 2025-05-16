package com.urban.mobility.topology.application

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestInbox}
import com.urban.mobility.shared.domain.model.VehiclePositionChanged
import com.urban.mobility.tracking.domain.TopologicalMapEntity.{Command, MoveVehicle}
import com.urban.mobility.topology.domain.model.{NetworkLocation, NetworkMap}
import com.urban.mobility.tracking.application.TrackingService
import com.vividsolutions.jts.geom.Coordinate
import org.scalatest.flatspec.AnyFlatSpecLike

import scala.concurrent.ExecutionContext

class TopologicalServiceSpec extends ScalaTestWithActorTestKit with AnyFlatSpecLike {

  "A TopologicalService" must "sends a MoveVehicle command to the entity on moveVehicle call" in {
    implicit val executor: ExecutionContext = system.executionContext

    // given
    val network = NetworkMap(
      Map(0 -> new Coordinate(0.0, 0.0), 1 -> new Coordinate(10.0, 0.0), 2 -> new Coordinate(10.0, 10.0)),
      List((0, 1), (1, 2), (2, 0)))

    val inbox = TestInbox[Command]()
    val service = TrackingService(inbox.ref, network)
    // when
    service.moveVehicle(VehiclePositionChanged("v1", new Coordinate(5.0, 0, 0)))
    // then
    inbox.expectMessage(MoveVehicle("v1", NetworkLocation((0, 1), 0.5)))
  }
}
