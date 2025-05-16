package com.urban.mobility.tracking.domain

import java.util.UUID

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import com.urban.mobility.shared.domain.model.Vehicle
import com.urban.mobility.shared.util.Serializable
import com.urban.mobility.topology.domain.model.{NetworkLocation, NetworkMap, TopologicalLocation, TopologicalMap}

import scala.concurrent.duration._

object TopologicalMapEntity {

  type ID = UUID
  type State = Option[TopologicalMap]

  sealed trait Command extends Serializable
  case class FromNetworkMap(network: NetworkMap) extends Command
  case class FromMap(map: TopologicalMap) extends Command
  case class GetMap(replyTo: ActorRef[CurrentMap]) extends Command
  case class CurrentMap(map: Option[TopologicalMap])
  case class GetVehicles(replyTo: ActorRef[CurrentVehicles]) extends Command
  case class CurrentVehicles(vehicles: Option[Map[Vehicle.ID, TopologicalLocation]])
  case class MoveVehicle(vehicle: Vehicle.ID, location: NetworkLocation) extends Command
  case class Stop(replyTo: Option[ActorRef[Confirmation]] = None) extends Command

  sealed trait Confirmation extends Serializable
  case object Done extends Confirmation
  case object Rejected extends Confirmation

  sealed trait Event extends Serializable {
    def id: ID
  }
  case class MapInitialized(id: ID, map: TopologicalMap) extends Event
  case class VehicleMoved(id: ID, vehicle: Vehicle.ID, location: TopologicalLocation) extends Event

  def apply(id: ID): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      PersistenceId("TopologicalMap", id.toString),
      None,
      (state, command) =>
        if (state.isEmpty) handleCommandWhenInitializing(id, state, command)
        else handleCommandWhenActive(id, state, command),
      (state, event) => handleEvent(state, event))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3))
      .onPersistFailure(SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))

  private def handleCommandWhenInitializing(id: ID, state: State, command: Command): Effect[Event, State] =
    command match {
      case FromNetworkMap(map) => Effect.persist(MapInitialized(id, TopologicalMap.from(map)))
      case FromMap(map) => Effect.persist(MapInitialized(id, map))

      case GetMap(replyTo) => replyTo ! CurrentMap(None)
        Effect.none
      case GetVehicles(replyTo) => replyTo ! CurrentVehicles(None)
        Effect.none
      case Stop(replyTo) => Effect.stop().thenRun(_ => replyTo.foreach(ref => ref ! Done))
      case _ => Effect.none
    }

  private def handleCommandWhenActive(id: ID, state: State, command: Command): Effect[Event, State] =
    command match {
      case GetMap(replyTo) => replyTo ! CurrentMap(state)
        Effect.none
      case GetVehicles(replyTo) => replyTo ! CurrentVehicles(state.map(_.vehiclesAsMap))
        Effect.none
      case MoveVehicle(vehicleID, location) => TopologicalLocation.from(location) match {
        case Some(loc) => Effect.persist(VehicleMoved(id, vehicleID, loc))
        case _ => Effect.none
      }
      case Stop(replyTo) => Effect.stop().thenRun(_ => replyTo.foreach(ref => ref ! Done))
      case _ => Effect.none
    }

  private def handleEvent(state: State, event: Event): State = {
    event match {
      case MapInitialized(_, map) => Some(map)
      case VehicleMoved(_, name, location) => state.map(_.moveVehicle(name, location))
      case _ => state
    }
  }
}
