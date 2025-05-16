package com.urban.mobility.topology.infra.config

import com.urban.mobility.topology.domain.model.NetworkMap
import com.urban.mobility.topology.infra.io.GeoJsonFileReader
import com.vividsolutions.jts.geom.Coordinate

case class TopologyConfiguration(network: NetworkMap)

object TopologyConfiguration {

  def apply(): NetworkMap = NetworkMap(
    nodes = Map(
      (1, new Coordinate(0, 0)),
      (2, new Coordinate(10, 0)),
      (3, new Coordinate(0, 10)),
      (4, new Coordinate(10, 10))
    ),
    edges = List(
      (1, 3),
      (3, 4),
      (4, 2),
      (2, 1)
    )
  )

  def apply(geoFilename: String): TopologyConfiguration = {
    val network = NetworkMap(GeoJsonFileReader(geoFilename))
    println(s"Nodes:${network.nodes}")
    println(s"Edges:${network.edges}")
    println(s"Paths:${network.allPathsToAnyNode}")
    TopologyConfiguration(network)
  }
}
