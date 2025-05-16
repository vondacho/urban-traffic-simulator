package com.urban.mobility.travelling.domain

import java.time.Instant

import com.urban.mobility.topology.domain.model.NetworkLocation
import com.urban.mobility.travelling.domain.TravelStatistics
import com.vividsolutions.jts.geom.Coordinate
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class TravelStatisticsSpec extends AnyFlatSpec with Matchers {

  "Statistics" must "be empty if no vehicle has moved on any edge" in {
    // given
    val statistics = TravelStatistics.empty
    // when
    // then
    statistics.asDistancePerVehicleMap.isEmpty must be(true)
    statistics.asAverageTimePerEdgeMap.isEmpty must be(true)
  }

  "Vehicle distance" must "is 0 when entering very first edge" in {
    // given
    val statistics = TravelStatistics.empty
    // when
    val result = statistics
      .addMove("v1", (new Coordinate(0.0, 0.0), NetworkLocation((0, 1), 0.5)), Instant.now())
      .addMove("v2", (new Coordinate(1.0, 1.0), NetworkLocation((1, 2), 0.5)), Instant.now())
    // then
    result.asDistancePerVehicleMap must be(Map("v1" -> 0.0, "v2" -> 0.0))
  }

  "Vehicle distance" must "equal to the sum of distance of each move step, it does not depend on the traversed edges" in {
    // given
    val statistics = TravelStatistics.empty
    // when
    val result = statistics
      .addMove("v1", (new Coordinate(0.0, 0.0), NetworkLocation((0, 1), 0.0)), Instant.now())
      .addMove("v1", (new Coordinate(1.0, 0.0), NetworkLocation((0, 1), 0.0)), Instant.now())
      .addMove("v1", (new Coordinate(1.0, 1.0), NetworkLocation((1, 2), 0.0)), Instant.now())
      .addMove("v2", (new Coordinate(1.0, 1.0), NetworkLocation((1, 2), 0.0)), Instant.now())
      .addMove("v2", (new Coordinate(1.0, 2.0), NetworkLocation((2, 3), 0.0)), Instant.now())
    // then
    result.asDistancePerVehicleMap must be(Map("v1" -> 2.0, "v2" -> 1.0))
  }

  "Edge average time" must "does not exist as long as no vehicle leaves it" in {
    // given
    val statistics = TravelStatistics.empty
    // when
    val result = statistics
      .addMove("v1", (new Coordinate(0.0, 0.0), NetworkLocation((0, 1), 0.0)), Instant.now())
      .addMove("v1", (new Coordinate(1.0, 0.0), NetworkLocation((0, 1), 1.0)), Instant.now())
      .addMove("v2", (new Coordinate(0.0, 0.0), NetworkLocation((0, 1), 0.0)), Instant.now())
      .addMove("v2", (new Coordinate(0.5, 0.0), NetworkLocation((0, 1), 0.5)), Instant.now())
    // then
    result.asAverageTimePerEdgeMap((0, 1)) must be(None)
  }

  "Edge average time" must "sum the travel times for an edge travelled by a single vehicle" in {
    // given
    val statistics = TravelStatistics.empty
    val startTime = Instant.now()
    // when
    val result = statistics
      .addMove("v1", (new Coordinate(0.0, 0.0), NetworkLocation((0, 1), 0.0)), startTime)
      .addMove("v1", (new Coordinate(0.5, 0.0), NetworkLocation((0, 1), 0.5)), startTime.plusSeconds(1))
      .addMove("v1", (new Coordinate(1.0, 0.0), NetworkLocation((0, 1), 1.0)), startTime.plusSeconds(2))
      .addMove("v1", (new Coordinate(1.0, 1.0), NetworkLocation((1, 2), 1.0)), startTime.plusSeconds(3))
    // then
    val res = result.asAverageTimePerEdgeMap
    res((0, 1)) must be(Some(3.0))
    res((1, 2)) must be(None)
  }

  "Edge average time" must "compute the average of the travel times for a single edge travelled by two vehicles" in {
    // given
    val statistics = TravelStatistics.empty
    val startTime = Instant.now()
    // when
    val result = statistics
      .addMove("v1", (new Coordinate(0.0, 0.0), NetworkLocation((0, 1), 0.0)), startTime)
      .addMove("v1", (new Coordinate(0.5, 0.0), NetworkLocation((0, 1), 0.5)), startTime.plusSeconds(1))
      .addMove("v1", (new Coordinate(1.0, 0.0), NetworkLocation((0, 1), 1.0)), startTime.plusSeconds(2))
      .addMove("v1", (new Coordinate(1.0, 1.0), NetworkLocation((1, 2), 1.0)), startTime.plusSeconds(3))
      .addMove("v2", (new Coordinate(0.0, 0.0), NetworkLocation((0, 1), 0.0)), startTime)
      .addMove("v2", (new Coordinate(1.0, 0.0), NetworkLocation((0, 1), 1.0)), startTime.plusSeconds(1))
      .addMove("v2", (new Coordinate(1.0, 1.0), NetworkLocation((1, 2), 1.0)), startTime.plusSeconds(2))
    // then
    val res = result.asAverageTimePerEdgeMap
    res((0, 1)) must be(Some(2.5))
    res((1, 2)) must be(None)
  }

  "Edge average time" must "care bidirectional travelling on a single edge by a single vehicle" in {
    // given
    val statistics = TravelStatistics.empty
    val startTime = Instant.now()
    // when
    val result = statistics
      .addMove("v1", (new Coordinate(0.0, 0.0), NetworkLocation((0, 1), 0.0)), startTime)
      .addMove("v1", (new Coordinate(1.0, 0.0), NetworkLocation((0, 1), 1.0)), startTime.plusSeconds(1))
      .addMove("v1", (new Coordinate(0.8, 0.0), NetworkLocation((0, 1), 0.8)), startTime.plusSeconds(2))
      .addMove("v1", (new Coordinate(0.5, 0.0), NetworkLocation((0, 1), 0.5)), startTime.plusSeconds(3))
      .addMove("v1", (new Coordinate(0.0, 0.0), NetworkLocation((0, 1), 0.0)), startTime.plusSeconds(4))
    // then
    val res = result.asAverageTimePerEdgeMap
    res((0, 1)) must be(Some(2.0))
    res((1, 0)) must be(Some(2.0))
  }
}
