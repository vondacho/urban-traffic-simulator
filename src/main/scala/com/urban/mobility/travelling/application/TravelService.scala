package com.urban.mobility.travelling.application

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.urban.mobility.shared.domain.model.{Vehicle, VehiclePositionChanged}
import com.urban.mobility.topology.domain.model.{Edge, NetworkMap, Node}
import com.urban.mobility.travelling.domain.TravelStatisticsEntity._
import com.urban.mobility.travelling.domain.Type.AverageTime

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

case class TravelService(registry: ActorRef[Command], network: NetworkMap)
                        (implicit val system: ActorSystem[Nothing], implicit val executor: ExecutionContext) {

  private implicit val askTimeout: Timeout = new Timeout(5.seconds)

  def getDistances: Future[Map[Vehicle.ID, Double]] = (registry ? GetDistances)
    .map({ case CurrentDistances(distances) => distances })

  def getAverageTimes: Future[Map[Edge.ID, Double]] = (registry ? GetAverageTimes)
    .map({ case CurrentAverageTimes(times) => times.filterNot({ case (_, v) => v.isEmpty })
      .map({ case (k, v) => (k, v.get) })
    })

  def moveVehicle: VehiclePositionChanged => Unit = {
    case VehiclePositionChanged(vehicleID, absolute, timestamp) =>
      network.toLocation(absolute) match {
        case Some(location) => registry ! MoveVehicle(vehicleID, (absolute, location), timestamp)
        case _ =>
      }
  }

  def estimatedArrivalTimeToAnyReachableNode(vehicle: Vehicle.ID): Future[Map[Node.ID, AverageTime]] =
    (registry ? GetStatistics).map({
      case CurrentStatistics(statistics) =>
        if (statistics.hasVehicle(vehicle))
          EATStatistic.compute(
            statistics.vehicleLastMoves(vehicle)._2,
            statistics.asAverageTimePerEdgeMap,
            network.allPathsToAnyNode)
        else
          Map()
    })
}
