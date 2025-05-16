package com.urban.mobility.travelling.domain

import java.time.Instant
import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.urban.mobility.topology.domain.model.NetworkLocation
import com.urban.mobility.travelling.domain.TravelStatisticsEntity
import com.urban.mobility.travelling.domain.TravelStatisticsEntity.{CurrentDistances, GetDistances, MoveVehicle}
import com.vividsolutions.jts.geom.Coordinate
import org.scalatest.flatspec.AnyFlatSpecLike

class TravelStatisticsEntitySpec extends ScalaTestWithActorTestKit with AnyFlatSpecLike {

  "TravelStatistics entity" must "return travel distance statistics on demand" in {
    // given
    val statisticsRef = spawn(TravelStatisticsEntity(UUID.randomUUID()))
    statisticsRef ! MoveVehicle("v1", (new Coordinate(1.0, 0.0), NetworkLocation((0,1), 0.5)), Instant.now())
    statisticsRef ! MoveVehicle("v2", (new Coordinate(3.0, 0.0), NetworkLocation((0,1), 0.5)), Instant.now())

    val probe = createTestProbe[CurrentDistances]()
    // when
    statisticsRef ! GetDistances(probe.ref)
    // then
    probe.expectMessage(CurrentDistances(Map("v1" -> 0.0, "v2" -> 0.0)))
  }
}
