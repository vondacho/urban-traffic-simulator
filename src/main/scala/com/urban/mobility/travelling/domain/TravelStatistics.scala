package com.urban.mobility.travelling.domain

import java.time.Instant

import Type._
import com.urban.mobility.shared.domain.model.Vehicle
import com.urban.mobility.shared.util.Geometry
import com.urban.mobility.topology.domain.model.{Edge, NetworkLocation}
import com.vividsolutions.jts.geom.Coordinate

object Type {
  type TimeInSeconds = Long
  type AverageTime = Double
  type Distance = Double
  type ArrivalTimestamp = Instant
  type LastMoveTimestamp = Instant
  type TravelTime = TimeInSeconds
  type TravelCount = Long
  type LastMove = (Coordinate, NetworkLocation, LastMoveTimestamp)
}

case class TravelStatistics(
                             vehicleLastMoves: Map[Vehicle.ID, LastMove],
                             vehicleCurrentEdge: Map[Vehicle.ID, (Edge.ID, ArrivalTimestamp)],
                             distancePerVehicle: Map[Vehicle.ID, Distance],
                             averageTimePerEdge: Map[Edge.ID, Option[(TravelTime, TravelCount)]],
                           ) {
  import Geometry._

  def hasVehicle(vehicle: Vehicle.ID): Boolean = vehicleLastMoves.get(vehicle).isDefined

  def asDistancePerVehicleMap: Map[Vehicle.ID, Distance] = distancePerVehicle

  def asAverageTimePerEdgeMap: Map[Edge.ID, Option[AverageTime]] = {
    averageTimePerEdge
      .map { case (edge, statistic) => (edge, statistic.map(computeAverageTime))
      }
  }

  def addMove(vehicle: Vehicle.ID, to: (Coordinate, NetworkLocation), timestamp: Instant): TravelStatistics = {
    val (currentAbsolute, currentEdge, currentPosition, currentTime) = (to._1, to._2.edge, to._2.position, timestamp)

    vehicleLastMoves.get(vehicle) match {
      case Some((previousPosition, previousEdge, previousTime)) =>
        updateVehicleDistance(vehicle, currentAbsolute, previousPosition)
          .updateEdgeTravelTime(vehicle, currentEdge, currentTime, previousEdge.edge, previousTime)
          .copy(
            vehicleLastMoves = vehicleLastMoves +
              (vehicle -> (currentAbsolute, NetworkLocation(currentEdge, currentPosition), currentTime)))

      case _ =>
        this.copy(
          distancePerVehicle = distancePerVehicle + (vehicle -> 0.0),
          averageTimePerEdge = putIfAbsent(averageTimePerEdge, currentEdge, None),
          vehicleLastMoves = vehicleLastMoves + (vehicle -> (currentAbsolute, NetworkLocation(currentEdge, currentPosition), currentTime)),
          vehicleCurrentEdge = vehicleCurrentEdge + (vehicle -> (currentEdge, currentTime)))
    }
  }

  def vehiclePosition(vehicle: Vehicle.ID): Option[(Edge.ID, Double, ArrivalTimestamp, LastMoveTimestamp)] =
    vehicleCurrentEdge.get(vehicle).map {
      case (edge, arrivalTime) => (
        edge,
        vehicleLastMoves(vehicle)._2.position,
        arrivalTime,
        vehicleLastMoves(vehicle)._3)
    }

  def edgeAverageTravelTime(edge: Edge.ID): Option[AverageTime] =
    averageTimePerEdge(edge).map(computeAverageTime)

  private def computeAverageTime(values: (TravelTime, TravelCount)) = values._1.toDouble / values._2.toDouble


  private def updateVehicleDistance(vehicle: Vehicle.ID,
                                    currentPosition: Coordinate,
                                    previousPosition: Coordinate): TravelStatistics =

    this.copy(distancePerVehicle =
      applyIfPresent[Vehicle.ID, Distance](distancePerVehicle, vehicle,
        acc => acc + euclidianDistance(previousPosition, currentPosition)))

  private def updateEdgeTravelTime(vehicle: Vehicle.ID,
                                   currentEdge: Edge.ID,
                                   currentTime: Instant,
                                   previousEdge: Edge.ID,
                                   previousTime: Instant): TravelStatistics = {

    val edgeHasBeenLeft = currentEdge != previousEdge

    if (edgeHasBeenLeft) {
      val edgeArrivalTime: ArrivalTimestamp = vehicleCurrentEdge.get(vehicle).map(_._2).getOrElse(currentTime)
      val estimatedTravelTime = currentTime.getEpochSecond - edgeArrivalTime.getEpochSecond

      this.copy(
        averageTimePerEdge =
          putIfAbsent(
            applyIfPresent[Edge.ID, Option[(TravelTime, TravelCount)]](averageTimePerEdge, previousEdge, {
              case Some((totalTravelTime, travelCount)) => Some(totalTravelTime + estimatedTravelTime, travelCount + 1)
              case _ => Some(estimatedTravelTime, 1L)
            }),
            currentEdge, None),

        vehicleCurrentEdge = vehicleCurrentEdge + (vehicle -> (currentEdge, currentTime)))

    } else this
  }

  private def applyIfPresent[T, V](map: Map[T, V], key: T, f: V => V): Map[T, V] = {
    map.get(key) match {
      case Some(value) => map + (key -> f(value))
      case _ => map
    }
  }

  private def putIfAbsent[T, V](map: Map[T, V], key: T, value: V): Map[T, V] = {
    map.get(key) match {
      case None => map + (key -> value)
      case _ => map
    }
  }
}

object TravelStatistics {
  def empty: TravelStatistics = TravelStatistics(Map(), Map(), Map(), Map())
}
