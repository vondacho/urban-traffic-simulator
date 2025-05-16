package com.urban.mobility.travelling.application

import com.urban.mobility.topology.domain.model.NetworkLocation
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class EATStatisticsSpec extends AnyFlatSpec with Matchers {

  "EAT" must "for the current edge is its average time multiplied by the current position" in {
    // given
    val vehiclePosition = NetworkLocation((0, 1), 0.5)
    val edgeAverageTime = Map((0, 1) -> Some(30.0))
    val singlePath = Map(0 -> Map(1 -> List(List((0, 1)))))
    // when
    val result = EATStatistic.compute(vehiclePosition, edgeAverageTime, singlePath)
    // then
    result(1) must be(15.0)
  }

  "EAT" must "for a sequence of edges is the sum of their average times" in {
    // given
    val edgeList = List((0, 1), (1, 2), (2, 3))
    val averageTimes = Map((0, 1) -> Some(30.0), (1, 2) -> Some(20.0), (2, 3) -> Some(10.0))
    // when
    val result = EATStatistic.computeEAT(edgeList, averageTimes, 5.0)
    // then
    result must be(Some(65.0))
  }

  "EAT" must "for the current edge and its counterpart is the sum of both average times" in {
    // given
    val vehiclePosition = NetworkLocation((0, 1), 0.5)
    val edgeAverageTime = Map((0, 1) -> Some(30.0), (1, 0) -> Some(20.0))
    val paths = Map(
      0 -> Map(1 -> List(List((0, 1)))),
      1 -> Map(0 -> List(List((1, 0)))))
    // when
    val result = EATStatistic.compute(vehiclePosition, edgeAverageTime, paths)
    // then
    result(1) must be(15.0)
    result(0) must be(15.0 + 20.0)
  }

  "EAT" must "for each reachable node is the sum of initial EAT, plus the EAT of the associated edge sequence to that node" in {
    // given
    val vehiclePosition = NetworkLocation((0, 1), 0.5)
    val edgeAverageTime = Map((0, 1) -> Some(30.0), (1, 2) -> Some(20.0), (2, 3) -> Some(10.0))
    val paths = Map(
      0 -> Map(1 -> List(List((0, 1))), 2 -> List(List((0, 1), (1, 2))), 3 -> List(List((0, 1), (1, 2), (2, 3)))),
      1 -> Map(2 -> List(List((1, 2))), 3 -> List(List((1, 2), (2, 3)))),
      2 -> Map(3 -> List(List((2, 3))))
    )
    // when
    val result = EATStatistic.compute(vehiclePosition, edgeAverageTime, paths)
    // then
    result(1) must be(15.0)
    result(2) must be(15.0 + 20.0)
    result(3) must be(15.0 + 20.0 + 10.0)
  }

  "EAT" must "for each reachable node is the sum of initial EAT, plus the minimum EAT among the associated edge sequences to that node" in {
    // given
    val vehiclePosition = NetworkLocation((0, 1), 0.5)
    val edgeAverageTime = Map((0, 1) -> Some(30.0), (1, 2) -> Some(20.0), (2, 3) -> Some(10.0), (1, 3) -> Some(10.0))
    val paths = Map(
      0 -> Map(
        1 -> List(List((0, 1))),
        2 -> List(List((0, 1), (1, 2))),
        3 -> List(List((0, 1), (1, 2), (2, 3)), List((0, 1), (1, 3)))
      ),
      1 -> Map(
        2 -> List(List((1, 2))),
        3 -> List(List((1, 2), (2, 3)), List((1, 3)))
      ),
      2 -> Map(3 -> List(List((2, 3))))
    )
    // when
    val result = EATStatistic.compute(vehiclePosition, edgeAverageTime, paths)
    // then
    result(1) must be(15.0)
    result(2) must be(15.0 + 20.0)
    result(3) must be(15.0 + 10.0)
  }
}
