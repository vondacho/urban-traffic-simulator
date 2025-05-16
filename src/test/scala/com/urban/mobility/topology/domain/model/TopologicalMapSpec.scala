package com.urban.mobility.topology.domain.model

import com.vividsolutions.jts.geom.Coordinate
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class TopologicalMapSpec extends AnyFlatSpec with Matchers {

  "A vehicle move" must "be correctly reflected on a topological map without vehicles" in {
    // given
    val map = TopologicalMap(
      Map(0 -> "0", 1 -> "1", 2 -> "2"),
      List((0, 1, 1.0), (1, 2, 1.0), (2, 0, 1.0)),
      List())
    // when
    val result = map.moveVehicle("v1", TopologicalLocation((0, 1), 0.5))
    // then
    result.vehicles.head must be(((0, 1), "v1", 0.5))
  }

  "A vehicle move" must "be correctly reflected on a topological map having only this vehicle" in {
    // given
    val map = TopologicalMap(
      Map(0 -> "0", 1 -> "1", 2 -> "2"),
      List((0, 1, 1.0), (1, 2, 1.0), (2, 0, 1.0)),
      List(((0, 1), "v1", 0.5)))
    // when
    val result = map.moveVehicle("v1", TopologicalLocation((1, 2), 0.75))
    // then
    result.vehicles must be(List(((1, 2), "v1", 0.75)))
  }

  "A vehicle move" must "be correctly reflected on a topological map having not only this vehicle" in {
    // given
    val map = TopologicalMap(
      Map(0 -> "0", 1 -> "1", 2 -> "2"),
      List((0, 1, 1.0), (1, 2, 1.0), (2, 0, 1.0)),
      List(((0, 1), "v1", 0.5), ((1, 2), "v2", 0.5)))
    // when
    val result = map.moveVehicle("v1", TopologicalLocation((1, 2), 0.75))
    // then
    result.vehicles must be(List(((1, 2), "v1", 0.75), ((1, 2), "v2", 0.5)))
  }

  "A vehicle move" must "not be reflected on a topological without the specified segment" in {
    // given
    val map = TopologicalMap(
      Map(0 -> "0", 1 -> "1"),
      List((0, 1, 1.0)),
      List(((0, 1), "v1", 0.5)))
    // when
    // then
    map.moveVehicle("v1", TopologicalLocation((1, 2), 0.75)).vehicles must be(List(((0, 1), "v1", 0.5)))
    map.moveVehicle("v2", TopologicalLocation((1, 2), 0.75)).vehicles must be(List(((0, 1), "v1", 0.5)))
  }

  "To an network location" must "maybe correspond a topological location" in {
    // given
    val networkLocation = NetworkLocation((0, 1), 0.5)
    // when
    val topologicalLocation = TopologicalLocation.from(networkLocation)
    // then
    topologicalLocation must be(Some(TopologicalLocation((0, 1), 0.5)))
  }

  "TopologicalMap map" must "be correctly initialized from network map" in {
    // given
    val network = NetworkMap(
      Map(0 -> new Coordinate(0.0, 0.0), 1 -> new Coordinate(10.0, 0.0), 2 -> new Coordinate(10.0, 10.0)),
      List((1, 2), (0, 1), (2, 0)))
    // when
    val map = TopologicalMap.from(network)
    // then
    map.stations.size must be(network.nodes.size)
    network.nodes.foreach { case (k, _) => map.stations.contains(k) must be(true) }
    map.segments.size must be(network.edges.size)
    map.vehicles.isEmpty must be(true)
  }
}
