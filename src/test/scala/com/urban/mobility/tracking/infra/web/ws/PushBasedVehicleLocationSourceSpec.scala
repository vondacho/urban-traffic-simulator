package com.urban.mobility.tracking.infra.web.ws

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.eventstream.EventStream
import akka.stream.scaladsl.Sink
import com.urban.mobility.shared.domain.model.VehiclePositionChanged
import com.urban.mobility.topology.domain.model.TopologicalLocation
import com.vividsolutions.jts.geom.Coordinate
import org.scalatest.concurrent.Waiters
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.collection.mutable

class PushBasedVehicleLocationSourceSpec extends AnyFlatSpec
  with Waiters
  with Matchers
  with VehicleLocationJson {

  lazy val testKit: ActorTestKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  "The source" must "listens to one vehicle position and send the mapped vehicle location to the sink" in {
    // given
    val toLocation: Coordinate => TopologicalLocation =
      coordinate => TopologicalLocation((coordinate.x.toInt, coordinate.y.toInt), 1.0)

    val waiter = new Waiter

    val source = PushBasedVehicleLocationSource(e => Some((e.vehicleID, toLocation(e.absolute))))
      .map(e => {
        // then
        waiter {
          e("vehicle") must be(TopologicalLocation((0, 1), 1.0))
        }
        waiter.dismiss()
        e
      })
    // when
    source.runWith(Sink.ignore)
    typedSystem.eventStream ! EventStream.Publish(VehiclePositionChanged("vehicle", new Coordinate(0.0, 1.0)))

    waiter.await()
  }

  "The source" must "listens to vehicles position events and summaries them to the sink" in {
    // given
    val toLocation: Coordinate => TopologicalLocation =
      coordinate => TopologicalLocation((coordinate.x.toInt, coordinate.y.toInt), coordinate.z)

    val waiter = new Waiter()
    val locationSummaries = mutable.ListBuffer.empty[Map[String, TopologicalLocation]]

    val source = PushBasedVehicleLocationSource(e => Some((e.vehicleID, toLocation(e.absolute))))
      .map(e => {
        locationSummaries += e
        waiter.dismiss()
        e
      })
    // when
    source.runWith(Sink.ignore)
    typedSystem.eventStream ! EventStream.Publish(VehiclePositionChanged("vehicle1", new Coordinate(2.5, 0.0, 0.25)))
    typedSystem.eventStream ! EventStream.Publish(VehiclePositionChanged("vehicle1", new Coordinate(5.0, 0.0, 0.5)))
    typedSystem.eventStream ! EventStream.Publish(VehiclePositionChanged("vehicle2", new Coordinate(10.0, 5.0, 0.5)))
    // then
    waiter.await(Dismissals(3))

    locationSummaries.size must be(3)
    locationSummaries.toList must be(List(
      Map("vehicle1" -> TopologicalLocation((2, 0), 0.25)),
      Map("vehicle1" -> TopologicalLocation((5, 0), 0.5)),
      Map("vehicle1" -> TopologicalLocation((5, 0), 0.5), "vehicle2" -> TopologicalLocation((10, 5), 0.5))))
  }
}
