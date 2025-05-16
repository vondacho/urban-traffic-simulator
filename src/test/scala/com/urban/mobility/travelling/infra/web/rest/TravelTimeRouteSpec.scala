package com.urban.mobility.travelling.infra.web.rest

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.urban.mobility.topology.domain.model.NetworkMap
import com.urban.mobility.travelling.application.TravelService
import com.urban.mobility.travelling.domain.TravelStatistics
import com.urban.mobility.travelling.domain.TravelStatisticsEntity.{Command, CurrentAverageTimes, GetAverageTimes}
import com.urban.mobility.travelling.domain.Type._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class TravelTimeRouteSpec extends AnyFlatSpec
  with Matchers
  with ScalatestRouteTest
  with TravelStatisticsJson {

  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  "The travel time api" must "return average time for every edge" in {
    import TravelTimeApi._

    // given
    val travelStatisticEntity: ActorRef[Command] = testKit.spawn(TravelStatisticEntityMock())
    val travelService: TravelService = TravelService(travelStatisticEntity, NetworkMap.empty)
    val route: Route = TravelTimeRoute(travelService)

    // when
    Get(s"/${API}/${API_VERSION}/${SEGMENT_COLLECTION}/${AVERAGE_TRAVEL_TIME_RESOURCE}") ~> route ~>
      // then
      check {
        status mustBe StatusCodes.OK
        contentType must ===(ContentTypes.`application/json`)
        val entity = entityAs[Map[String, AverageTime]]
        entity(Sample.EDGE_UT.toString) must
          be(Sample.EDGE_TRAVEL_TOTAL_TIME.toDouble / Sample.EDGE_TRAVEL_COUNT.toDouble)
      }
  }

  // given
  private object Sample {
    val EDGE_UT = (0, 1)
    val EDGE_TRAVEL_TOTAL_TIME = 100L
    val EDGE_TRAVEL_COUNT = 10L

    val statistics = TravelStatistics(
      vehicleLastMoves = Map(),
      vehicleCurrentEdge = Map(),
      distancePerVehicle = Map(),
      averageTimePerEdge = Map(EDGE_UT -> Some((EDGE_TRAVEL_TOTAL_TIME, EDGE_TRAVEL_COUNT))))
  }

  // given
  private object TravelStatisticEntityMock {
    def apply(): Behavior[Command] =
      Behaviors.receiveMessage[Command] {
        case GetAverageTimes(replyTo) => replyTo ! CurrentAverageTimes(Sample.statistics.asAverageTimePerEdgeMap)
          Behaviors.same
        case _ =>
          Behaviors.same
      }
  }

}
