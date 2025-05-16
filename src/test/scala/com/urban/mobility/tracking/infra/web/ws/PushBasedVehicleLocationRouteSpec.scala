package com.urban.mobility.tracking.infra.web.ws

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.eventstream.EventStream
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import com.urban.mobility.shared.domain.model.VehiclePositionChanged
import com.urban.mobility.topology.domain.model.NetworkMap
import com.urban.mobility.tracking.application.TrackingService
import com.urban.mobility.tracking.domain.TopologicalMapEntity.Command
import com.vividsolutions.jts.geom.Coordinate
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class PushBasedVehicleLocationRouteSpec extends AnyFlatSpec
  with Matchers
  with ScalatestRouteTest
  with VehicleLocationJson {

  import VehicleLocationApi._

  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  "The websocket api" must "push the location of every vehicle" in {
    // given
    val network = NetworkMap(
      Map(0 -> new Coordinate(0.0, 0.0), 1 -> new Coordinate(10.0, 0.0), 2 -> new Coordinate(10.0, 10.0)),
      List((1, 2), (0, 1), (2, 0)))

    val wsClient = WSProbe()
    val entityProbe = testKit.createTestProbe[Command]
    val trackingService: TrackingService = TrackingService(entityProbe.ref, network)
    val route = VehicleLocationRoute.pushBased(trackingService)

    // when
    typedSystem.eventStream ! EventStream.Publish(VehiclePositionChanged("vehicle", new Coordinate(5.0, 0.0)))

    WS(s"/${API}/${API_VERSION_V2}/${VEHICLE_COLLECTION}/${ALL_LOCATIONS_RESOURCE}", wsClient.flow) ~> route ~>
      // then
      check {
        isWebSocketUpgrade mustEqual true
        wsClient.expectMessage("{\"vehicle\":{\"position\":0.5,\"segment\":[0,1]}}")
      }
  }
}
