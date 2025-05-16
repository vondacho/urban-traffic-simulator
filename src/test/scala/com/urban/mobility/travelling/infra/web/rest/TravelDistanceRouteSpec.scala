package com.urban.mobility.travelling.infra.web.rest

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.urban.mobility.shared.domain.model.Vehicle
import com.urban.mobility.topology.domain.model.NetworkMap
import com.urban.mobility.travelling.application.TravelService
import com.urban.mobility.travelling.domain.TravelStatistics
import com.urban.mobility.travelling.domain.TravelStatisticsEntity.{Command, CurrentDistances, GetDistances}
import com.urban.mobility.travelling.domain.Type._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class TravelDistanceRouteSpec extends AnyFlatSpec
  with Matchers
  with ScalatestRouteTest
  with TravelStatisticsJson {

  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  "The travel distance api" must "return total travelled distance for every vehicle" in {
    import TravelDistanceApi._

    // given
    val travelStatisticEntity: ActorRef[Command] = testKit.spawn(TravelStatisticEntityMock())
    val travelService: TravelService = TravelService(travelStatisticEntity, NetworkMap.empty)
    val route: Route = TravelDistanceRoute(travelService)

    // when
    Get(s"/${API}/${API_VERSION}/${VEHICLE_COLLECTION}/${TRAVEL_DISTANCES_RESOURCE}") ~> route ~>
      // then
      check {
        status mustBe StatusCodes.OK
        contentType must ===(ContentTypes.`application/json`)
        val entity = entityAs[Map[Vehicle.ID, Distance]]
        entity(Sample.VEHICLE_UT) must be(Sample.VEHICLE_TRAVEL_DISTANCE)
      }
  }

  // given
  private object Sample {
    val VEHICLE_UT = "1"
    val VEHICLE_TRAVEL_DISTANCE = 100.0

    val statistics = TravelStatistics(
      vehicleLastMoves = Map(),
      vehicleCurrentEdge = Map(),
      distancePerVehicle = Map(VEHICLE_UT -> VEHICLE_TRAVEL_DISTANCE),
      averageTimePerEdge = Map())
  }

  // given
  private object TravelStatisticEntityMock {
    def apply(): Behavior[Command] =
      Behaviors.receiveMessage[Command] {
        case GetDistances(replyTo) => replyTo ! CurrentDistances(Sample.statistics.asDistancePerVehicleMap)
          Behaviors.same
        case _ =>
          Behaviors.same
      }
  }

}
