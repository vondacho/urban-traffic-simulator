package com.urban.mobility.tracking.infra.web.ws

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import com.urban.mobility.tracking.domain.TopologicalMapEntity.{Command, CurrentVehicles, GetVehicles}
import com.urban.mobility.topology.domain.model.{NetworkMap, TopologicalLocation}
import com.urban.mobility.tracking.application.TrackingService
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class PullBasedVehicleLocationRouteSpec extends AnyFlatSpec
  with Matchers
  with ScalatestRouteTest
  with VehicleLocationJson {

  import VehicleLocationApi._

  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  "The websocket api" must "push the location of every vehicle" in {
    // given
    val wsClient = WSProbe()
    val topologicalMapEntity: ActorRef[Command] = testKit.spawn(TopologicalMapEntityMock())
    val trackingService: TrackingService = TrackingService(topologicalMapEntity, NetworkMap.empty)
    val route = VehicleLocationRoute.pullBased(trackingService)

    // when
    WS(s"/${API}/${API_VERSION_V1}/${VEHICLE_COLLECTION}/${ALL_LOCATIONS_RESOURCE}", wsClient.flow) ~> route ~>
      // then
      check {
        isWebSocketUpgrade mustEqual true
        wsClient.expectMessage("{\"vehicle\":{\"position\":0.25,\"segment\":[1,2]}}")
      }
  }

  // given
  private object TopologicalMapEntityMock {
    def apply(): Behavior[Command] =
      Behaviors.receiveMessage[Command] {
        case GetVehicles(replyTo) =>
          replyTo ! CurrentVehicles(Some(Map("vehicle" -> TopologicalLocation((1, 2), 0.25))))
          Behaviors.same
        case _ =>
          Behaviors.same
      }
  }
}
