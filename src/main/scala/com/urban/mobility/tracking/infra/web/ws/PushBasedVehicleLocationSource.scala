package com.urban.mobility.tracking.infra.web.ws

import java.time.Instant

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.eventstream.EventStream
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import com.urban.mobility.shared.domain.model.{Vehicle, VehicleEvent, VehiclePositionChanged}
import com.urban.mobility.topology.domain.model._

object PushBasedVehicleLocationSource {

  case class Complete(vehicleID: Vehicle.ID, timestamp: Instant) extends VehicleEvent
  case class Fail(vehicleID: Vehicle.ID, timestamp: Instant, ex: Exception) extends VehicleEvent

  def apply(handler: VehiclePositionChanged => Option[(Vehicle.ID, TopologicalLocation)])
           (implicit system: ActorSystem[Nothing]): Source[Map[Vehicle.ID, TopologicalLocation], NotUsed] = {

    val (actorRef, publisher) =
      ActorSource.actorRef[VehicleEvent](
        { case Complete(_, _) => },
        { case Fail(_, _, ex) => ex },
        bufferSize = 16,
        overflowStrategy = OverflowStrategy.dropHead)
        .toMat(Sink.asPublisher(false))(Keep.both)
        .run()

    val origin: Source[Map[Vehicle.ID, TopologicalLocation], NotUsed] = Source.fromPublisher(publisher)
      .map({ case e@VehiclePositionChanged(vehicleID, _, _) => handler(e) match {
        case Some((_, location)) => Map(vehicleID -> location)
        case None => Map()
      }
      })

    val statefulSource: Source[Map[Vehicle.ID, TopologicalLocation], NotUsed] = origin.statefulMapConcat({
      var state = Map[String, TopologicalLocation]()
      () =>
        vehicleLocation => {
          state = state ++ vehicleLocation
          List(state)
        }
    })

    system.eventStream ! EventStream.Subscribe(actorRef)

    statefulSource
  }
}
