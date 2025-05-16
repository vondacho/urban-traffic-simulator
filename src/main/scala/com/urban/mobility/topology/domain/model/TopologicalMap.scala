package com.urban.mobility.topology.domain.model

import com.urban.mobility.shared.domain.model.Vehicle

object Station {
  type ID = Int
  type Name = String
}

/**
 * A topological map encode an abstract representation of the transport network.
 *
 * Only stations, segments (connections between stations) and vehicles are displayed.
 *
 * Segment size is relative (fixed or proportional) and does not reflect the scale of the real world network.
 *
 * Example: https://en.wikipedia.org/wiki/Topological_map
 **/
case class TopologicalMap(
                           // Stations with their name.
                           // Int    - Station Identifier
                           // String - Station Name
                           stations: Map[Station.ID, Station.Name],
                           // Directed segments connecting stations.
                           // Int    - Source station identifier
                           // Int    - Target station identifier
                           // Double - Relative size (between 0.0 and 1.0)
                           segments: List[(Station.ID, Station.ID, Double)],
                           // Vehicles with their name and positions.
                           // (Int, Int) - Segment identifier (source, target) where the vehicle is currently located
                           // String     - Name of the vehicle
                           // Double     - Position of the vehicle relative to this segment (between 0.0 and 1.0)
                           vehicles: List[((Station.ID, Station.ID), Vehicle.ID, Double)]
                         ) {

  def moveVehicle(vehicle: Vehicle.ID, location: NetworkLocation): TopologicalMap =
    TopologicalLocation.from(location) match {
      case Some(loc) => moveVehicle(vehicle, loc)
      case _ => this
    }

  def moveVehicle(vehicle: Vehicle.ID, location: TopologicalLocation): TopologicalMap = {
    val TopologicalLocation(newSegment, newPosition) = location

    segmentAt(newSegment) match {
      case None => this
      case Some(_) => copy(
        stations,
        segments,
        (newSegment, vehicle, newPosition) :: vehicles.filterNot(v => v._2 == vehicle))
    }
  }

  def addVehicles(vehicles: Map[Vehicle.ID, TopologicalLocation]): TopologicalMap = this

  def vehiclesAsMap: Map[Vehicle.ID, TopologicalLocation] =
    vehicles.map(v => (v._2, TopologicalLocation(v._1, v._3))).toMap

  private def segmentAt(segment: (Station.ID, Station.ID)): Option[(Station.ID, Station.ID, Double)] =
    segments.find { case (n1, n2, _) => (n1, n2) == segment }
}

object TopologicalMap {
  private val FIXED_SEGMENT_RELATIVE_SIZE = 1.0

  def from(network: NetworkMap): TopologicalMap = {
    TopologicalMap(
      stationList(network),
      segmentList(network),
      Nil)
  }

  def from(network: NetworkMap, vehicles: Map[Vehicle.ID, TopologicalLocation]): TopologicalMap = {
    TopologicalMap(
      stationList(network),
      segmentList(network),
      Nil)
  }

  def empty = new TopologicalMap(Map(), List(), List())

  private def stationList(networkMap: NetworkMap): Map[Station.ID, Station.Name] =
    networkMap.nodes.map { case (stationId, _) => (stationId, s"s$stationId") }

  private def segmentList(networkMap: NetworkMap): List[(Station.ID, Station.ID, Double)] =
    networkMap.edges.map(edge => (edge._1, edge._2, FIXED_SEGMENT_RELATIVE_SIZE))
}

case class TopologicalLocation(segment: (Station.ID, Station.ID), position: Double)

object TopologicalLocation {
  def from(location: NetworkLocation): Option[TopologicalLocation] =
    Some(TopologicalLocation(location.edge, location.position))
}
