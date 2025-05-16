package com.urban.mobility.travelling.infra.web.rest

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.urban.mobility.topology.domain.model.{NetworkLocation, NetworkMap}
import com.urban.mobility.travelling.application.TravelService
import com.urban.mobility.travelling.domain.TravelStatistics
import com.urban.mobility.travelling.domain.TravelStatisticsEntity.{Command, CurrentStatistics, GetStatistics}
import com.urban.mobility.travelling.domain.Type._
import com.vividsolutions.jts.geom.Coordinate
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ArrivalTimeRouteSpec extends AnyFlatSpec
  with Matchers
  with ScalatestRouteTest
  with TravelStatisticsJson {

  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  "The estimated arrival time api" must "return arrival time for a given vehicle v1 to any reachable station" in {
    import ArrivalTimeApi._

    // given
    val travelStatisticEntity: ActorRef[Command] = testKit.spawn(TravelStatisticEntityMock())
    val travelService: TravelService = TravelService(travelStatisticEntity, NetworkMap.empty)
    val route: Route = ArrivalTimeRoute(travelService)

    // when
    Get(s"/${API}/${API_VERSION}/${VEHICLE_COLLECTION}/${Sample.VEHICLE_UT}/${ARRIVAL_TIMES_RESOURCE}") ~> route ~>
      // then
      check {
        status mustBe StatusCodes.OK
        contentType must ===(ContentTypes.`application/json`)
        val entity = entityAs[Map[String, AverageTime]]
        entity(Sample.NODE_UT.toString) must
          be((Sample.EDGE_TRAVEL_TOTAL_TIME / Sample.EDGE_TRAVEL_COUNT) * Sample.VEHICLE_POSITION)
      }
  }

  // given
  private object Sample {
    val VEHICLE_UT = "v1"
    val NODE_UT = 1
    val EDGE_UT = (0, NODE_UT)
    val EDGE_TRAVEL_TOTAL_TIME = 100L
    val EDGE_TRAVEL_COUNT = 10L
    val VEHICLE_POSITION = 0.5

    val statistics = TravelStatistics(
      vehicleLastMoves = Map(VEHICLE_UT -> (new Coordinate(0.5, 0.0), NetworkLocation((0, 1), VEHICLE_POSITION), Instant.now)),
      vehicleCurrentEdge = Map(),
      distancePerVehicle = Map(),
      averageTimePerEdge = Map(EDGE_UT -> Some((EDGE_TRAVEL_TOTAL_TIME, EDGE_TRAVEL_COUNT))))
  }

  // given
  private object TravelStatisticEntityMock {
    def apply(): Behavior[Command] =
      Behaviors.receiveMessage[Command] {
        case GetStatistics(replyTo) => replyTo ! CurrentStatistics(Sample.statistics)
          Behaviors.same
        case _ =>
          Behaviors.same
      }
  }

}
