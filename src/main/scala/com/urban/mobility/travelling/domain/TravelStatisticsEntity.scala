package com.urban.mobility.travelling.domain

import java.time.Instant
import java.util.UUID

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import com.urban.mobility.shared.domain.model.Vehicle
import com.urban.mobility.shared.util.Serializable
import com.urban.mobility.topology.domain.model.{Edge, NetworkLocation, NetworkMap}
import com.vividsolutions.jts.geom.Coordinate

import scala.concurrent.duration._

object TravelStatisticsEntity {

  type ID = UUID
  type State = TravelStatistics

  sealed trait Command extends Serializable
  case class MoveVehicle(vehicle: Vehicle.ID, to: (Coordinate, NetworkLocation), timestamp: Instant) extends Command
  case class GetDistances(replyTo: ActorRef[CurrentDistances]) extends Command
  case class GetAverageTimes(replyTo: ActorRef[CurrentAverageTimes]) extends Command
  case class GetStatistics(replyTo: ActorRef[CurrentStatistics]) extends Command
  case class CurrentDistances(totalDistances: Map[Vehicle.ID, Double])
  case class CurrentAverageTimes(averageTimes: Map[Edge.ID, Option[Double]])
  case class CurrentStatistics(statistics: TravelStatistics)
  case class Stop(replyTo: Option[ActorRef[Confirmation]] = None) extends Command

  sealed trait Confirmation extends Serializable
  case object Done extends Confirmation
  case object Rejected extends Confirmation

  sealed trait Event extends Serializable {
    def id: ID
  }
  case class VehicleMoved(id: ID, vehicleID: Vehicle.ID, to: (Coordinate, NetworkLocation), timestamp: Instant) extends Event

  def apply(id: ID): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId("TravelStatistics", id.toString),
      TravelStatistics.empty,
      (state, command) => handleCommand(id, state, command),
      (state, event) => handleEvent(state, event))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))

  private def handleCommand(id: ID, state: State, command: Command): Effect[Event, State] =
    command match {
      case GetDistances(replyTo: ActorRef[CurrentDistances]) =>
        replyTo ! CurrentDistances(state.asDistancePerVehicleMap)
        Effect.none

      case GetAverageTimes(replyTo: ActorRef[CurrentAverageTimes]) =>
        replyTo ! CurrentAverageTimes(state.asAverageTimePerEdgeMap)
        Effect.none

      case GetStatistics(replyTo: ActorRef[CurrentStatistics]) =>
        replyTo ! CurrentStatistics(state.copy())
        Effect.none

      case MoveVehicle(vehicleID, to, timestamp) => Effect.persist(
        VehicleMoved(id, vehicleID, to, timestamp))

      case _ => Effect.none
    }

  private def handleEvent(state: State, event: Event): State = {
    event match {
      case VehicleMoved(_, vehicleID, to, timestamp) => state.addMove(vehicleID, to, timestamp)
      case _ => state
    }
  }
}
