package com.urban.mobility.tracking.infra.web.rest

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.urban.mobility.shared.domain.model.Vehicle
import com.urban.mobility.topology.domain.model.{NetworkMap, TopologicalLocation, TopologicalMap}
import com.urban.mobility.tracking.application.TrackingService
import com.urban.mobility.tracking.domain.TopologicalMapEntity._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class TopologicalMapRouteSpec extends AnyFlatSpec
  with Matchers
  with ScalatestRouteTest
  with TopologicalMapJson {

  import TopologicalMapApi._

  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  "The TopologicalMap api" must "return the topological map" in {
    // given
    val topologicalMapEntity: ActorRef[Command] = testKit.spawn(TopologicalMapEntityMock())
    val trackingService: TrackingService = TrackingService(topologicalMapEntity, NetworkMap.empty)
    val route: Route = TopologicalMapRoute(trackingService)

    // when
    Get(s"/${API}/${API_VERSION}/${MAP_RESOURCE}") ~> route ~>
      // then
      check {
        status mustBe StatusCodes.OK
        contentType must ===(ContentTypes.`application/json`)
        val entity = entityAs[TopologicalMapDto]
        entity.stations.size mustBe Sample.map.stations.size
        entity.segments mustEqual Sample.map.segments
        entity.vehicles mustEqual Sample.map.vehicles
      }
  }

  "The TopologicalMap api" must "return the location of the vehicles on the topological map" in {
    // given
    val topologicalMapEntity: ActorRef[Command] = testKit.spawn(TopologicalMapEntityMock())
    val trackingService: TrackingService = TrackingService(topologicalMapEntity, NetworkMap.empty)
    val route: Route = TopologicalMapRoute(trackingService)

    // when
    Get(s"/${API}/${API_VERSION}/${MAP_RESOURCE}/${ALL_VEHICLE_LOCATION_RESOURCE}") ~> route ~>
      // then
      check {
        status mustBe StatusCodes.OK
        contentType must ===(ContentTypes.`application/json`)
        val entity = entityAs[Map[Vehicle.ID, TopologicalLocation]]
        entity mustEqual Sample.map.vehiclesAsMap
      }
  }

  // given
  private object Sample {
    val map = TopologicalMap(
      Map(1 -> "1", 2 -> "2"),
      List((1, 2, 0.5)),
      List(((1, 2), "vehicle", 0.25)))
  }

  // given
  private object TopologicalMapEntityMock {
    def apply(): Behavior[Command] =
      Behaviors.receiveMessage[Command] {
        case GetMap(replyTo) =>
          replyTo ! CurrentMap(Some(Sample.map))
          Behaviors.same
        case _ =>
          Behaviors.same
      }
  }
}

