package com.urban.mobility.tracking.infra.web.rest

import com.urban.mobility.shared.domain.model.Vehicle
import com.urban.mobility.topology.domain.model.{TopologicalLocation, TopologicalMap}

case class TopologicalMapDto(
                              stations: Map[String, String],
                              segments: List[(Int, Int, Double)],
                              vehicles: List[((Int, Int), Vehicle.ID, Double)])

object TopologicalMapAdapter {
  def apply(map: Option[TopologicalMap]): TopologicalMapDto = map match {
    case Some(m) =>
      TopologicalMapDto(
        m.stations.map { case (key, value) => (key.toString, value) },
        m.segments,
        m.vehicles)
    case _ =>
      TopologicalMapDto(Map(), List(), List())
  }

  def vehicles(map: Option[TopologicalMap]): Map[Vehicle.ID, TopologicalLocation] = map match {
    case Some(m) => m.vehiclesAsMap
    case _ => Map()
  }
}
