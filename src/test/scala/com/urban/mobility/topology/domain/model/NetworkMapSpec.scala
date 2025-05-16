package com.urban.mobility.topology.domain.model

import com.vividsolutions.jts.geom.Coordinate
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class NetworkMapSpec extends AnyFlatSpec with Matchers {

  "To an absolute position" must "maybe corresponds a local position on an edge" in {
    // given
    val network = NetworkMap(
      Map(0 -> new Coordinate(0.0, 0.0), 1 -> new Coordinate(10.0, 0.0), 2 -> new Coordinate(10.0, 10.0)),
      List((1, 2), (0, 1), (2, 0)))
    // when
    // then
    network.toLocation(new Coordinate(5.0, 0.0)) must be(Some(NetworkLocation((0, 1), 0.5)))
    network.toLocation(new Coordinate(9.0, 0.0)) must be(Some(NetworkLocation((0, 1), 0.9)))
    network.toLocation(new Coordinate(10.0, 5.0)) must be(Some(NetworkLocation((1, 2), 0.5)))
    network.toLocation(new Coordinate(10.0, 9.0)) must be(Some(NetworkLocation((1, 2), 0.9)))
    network.toLocation(new Coordinate(5.0, 5.0)) must be(Some(NetworkLocation((2, 0), 0.5)))
    network.toLocation(new Coordinate(9.0, 9.0)) must be(Some(NetworkLocation((2, 0), 0.1)))
    network.toLocation(new Coordinate(0.1, 0.1)) must be(Some(NetworkLocation((2, 0), 0.99)))

    network.toLocation(new Coordinate(0.0, 0.0)) must be(Some(NetworkLocation((0, 1), 0)))
    network.toLocation(new Coordinate(10.0, 0.0)) must be(Some(NetworkLocation((1, 2), 0)))
    network.toLocation(new Coordinate(10.0, 10.0)) must be(Some(NetworkLocation((2, 0), 0)))
  }

  "Network map" must "be composed from given three segment list" in {
    // given
    val segmentList = List(
      ((0.0, 0.0), (10.0, 0.0)),
      ((10.0, 0.0), (10.0, 10.0)),
      ((10.0, 10.0), (0.0, 0.0)))
    // when
    val network = NetworkMap(segmentList)
    // then
    network.nodes.size must be(3)
    network.edges.size must be(3)
    network.nodes must be(Map(0 -> new Coordinate(0.0, 0.0), 1 -> new Coordinate(10.0, 0.0), 2 -> new Coordinate(10.0, 10.0)))
    network.edges must be(List((0, 1), (1, 2), (2, 0)))
  }

  "Network map" must "be empty if composed from a given empty segment list" in {
    // given
    val segmentList = Nil
    // when
    val network = NetworkMap(segmentList)
    // then
    network.nodes.isEmpty must be(true)
    network.edges.isEmpty must be(true)
  }

  "Network map" must "return one path between the nodes of a directed edge" in {
    // given
    val network = NetworkMap(
      Map(0 -> new Coordinate(0.0, 0.0), 1 -> new Coordinate(10.0, 0.0)),
      List((0, 1))
    )
    // when
    val paths: Map[Node.ID, List[List[Edge.ID]]] = network.allPathsToAnyNode(0)
    // then
    paths.size must be(1)
    paths(1) must be(List(List((0, 1))))
  }

  "Network map" must "return all paths from one node to any other distant node" in {
    // given
    val network = NetworkMap(
      Map(
        0 -> new Coordinate(0.0, 0.0),
        1 -> new Coordinate(10.0, 0.0),
        2 -> new Coordinate(20.0, 0.0)),
      List((0, 1), (1, 2))
    )
    // when
    val paths: Map[Node.ID, List[List[Edge.ID]]] = network.allPathsToAnyNode(0)
    // then
    paths.size must be(2)
    paths(1) must be(List(List((0, 1))))
    paths(2) must be(List(List((0, 1), (1, 2))))
  }

  "Network map" must "return all paths on a single bidirectional edge" in {
    // given
    val network = NetworkMap(
      Map(0 -> new Coordinate(0.0, 0.0), 1 -> new Coordinate(10.0, 0.0)),
      List((0, 1), (1, 0))
    )
    // when
    val paths: Map[Node.ID, Map[Node.ID, List[List[Edge.ID]]]] = network.allPathsToAnyNode
    // then
    paths.size must be(2)
    paths(0)(1) must be(List(List((0, 1))))
    paths(1)(0) must be(List(List((1, 0))))
  }

}
