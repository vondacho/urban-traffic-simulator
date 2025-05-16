package com.urban.mobility.topology.domain

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.eventstream.EventStream
import akka.persistence.journal.inmem.InmemJournal
import com.urban.mobility.tracking.domain.TopologicalMapEntity._
import com.urban.mobility.topology.domain.model.{NetworkLocation, TopologicalLocation, TopologicalMap}
import com.urban.mobility.tracking.domain
import com.urban.mobility.tracking.domain.TopologicalMapEntity
import org.scalatest.wordspec.AnyWordSpecLike

class TopologicalMapEntitySpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "A TopologicalMap entity" must {

    "reply a CurrentMap message to the sender when receiving a GetMap message" in {
      // given
      val map = TopologicalMap(
        Map(0 -> "0", 1 -> "1"),
        List((0, 1, 1.0)),
        List(((0, 1), "v1", 0.5)))

      val registryRef = spawn(TopologicalMapEntity(UUID.randomUUID()))

      registryRef ! FromMap(map)

      val probe = createTestProbe[CurrentMap]()
      // when
      registryRef ! GetMap(probe.ref)
      // then
      probe.expectMessage(CurrentMap(Some(map)))
    }

    "update the location of the given existing vehicle on its map when receiving a MoveVehicle message" in {
      // given
      val map = TopologicalMap(
        Map(0 -> "0", 1 -> "1"),
        List((0, 1, 1.0)),
        List(((0, 1), "v1", 0.5)))

      val registryRef = spawn(domain.TopologicalMapEntity(UUID.randomUUID()))

      registryRef ! FromMap(map)

      val probe = createTestProbe[CurrentVehicles]()
      // when
      registryRef ! MoveVehicle("v1", NetworkLocation((0, 1), 1.0))
      registryRef ! GetVehicles(probe.ref)
      // then
      probe.expectMessage(CurrentVehicles(Some(Map("v1" -> TopologicalLocation((0, 1), 1.0)))))
    }

    "add the location of the given unknown vehicle to the map when receiving a MoveVehicle message" in {
      // given
      val map = TopologicalMap(
        Map(0 -> "0", 1 -> "1"),
        List((0, 1, 1.0)),
        List(((0, 1), "v1", 0.5)))

      val registryRef = spawn(domain.TopologicalMapEntity(UUID.randomUUID()))

      registryRef ! FromMap(map)

      val probe = createTestProbe[CurrentVehicles]()
      // when
      registryRef ! MoveVehicle("v2", NetworkLocation((0, 1), 1.0))
      registryRef ! GetVehicles(probe.ref)
      // then
      probe.expectMessage(CurrentVehicles(Some(Map(
        "v1" -> TopologicalLocation((0, 1), 0.5),
        "v2" -> TopologicalLocation((0, 1), 1.0)))))
    }

    "reply a CurrentVehicles message to the sender when it receives a GetVehicles message" in {
      // given
      val map = TopologicalMap(
        Map(0 -> "0", 1 -> "1"),
        List((0, 1, 1.0)),
        List(((0, 1), "v1", 0.5)))

      val registryRef = spawn(domain.TopologicalMapEntity(UUID.randomUUID()))

      registryRef ! FromMap(map)

      val probe = createTestProbe[CurrentVehicles]()
      // when
      registryRef ! GetVehicles(probe.ref)
      // then
      probe.expectMessage(CurrentVehicles(Some(Map("v1" -> TopologicalLocation((0, 1), 0.5)))))
    }
  }

  "Persistent TopologicalMap entity" must {

    "source an event when initializing the registry state" in {
      // given
      val eventProbe = createTestProbe[InmemJournal.Operation]()
      system.eventStream ! EventStream.Subscribe(eventProbe.ref)

      val map = TopologicalMap(
        Map(0 -> "0", 1 -> "1"),
        List((0, 1, 1.0)),
        List(((0, 1), "vehicle", 0.5)))

      val registryRef = spawn(domain.TopologicalMapEntity(UUID.randomUUID()))

      // when
      registryRef ! FromMap(map)
      // then
      eventProbe.expectMessageType[InmemJournal.Write].event === (MapInitialized)
    }

    "source an event when moving a vehicle" in {
      // given
      val eventProbe = createTestProbe[InmemJournal.Operation]()
      system.eventStream ! EventStream.Subscribe(eventProbe.ref)

      val map = TopologicalMap(
        Map(0 -> "0", 1 -> "1"),
        List((0, 1, 1.0)),
        List(((0, 1), "vehicle", 0.5)))

      val registryRef = spawn(domain.TopologicalMapEntity(UUID.randomUUID()))

      // when
      registryRef ! FromMap(map)
      registryRef ! MoveVehicle("vehicle", NetworkLocation((0, 1), 0.0))
      // then
      eventProbe.expectMessageType[InmemJournal.Write].event === (MapInitialized)
      eventProbe.expectMessageType[InmemJournal.Write].event === (VehicleMoved)
    }

    "keep its state when restarted" in {
      // given
      val map = TopologicalMap(
        Map(0 -> "0", 1 -> "1"),
        List((0, 1, 1.0)),
        List(((0, 1), "vehicle", 0.5)))

      val testID = UUID.randomUUID()
      val registryRef = spawn(domain.TopologicalMapEntity(testID))
      registryRef ! FromMap(map)

      // when
      val stopProbe = testKit.createTestProbe[Confirmation]
      registryRef ! Stop(Some(stopProbe.ref))
      stopProbe.expectMessage(Done)

      val restartedRegistryRef = spawn(domain.TopologicalMapEntity(testID))

      // then
      val stateProbe = testKit.createTestProbe[CurrentMap]
      restartedRegistryRef ! GetMap(stateProbe.ref)
      stateProbe.expectMessage(CurrentMap(Some(map)))
    }

    "replay its state when restarted" in {
      // given
      val map = TopologicalMap(
        Map(0 -> "0", 1 -> "1"),
        List((0, 1, 1.0)),
        List(((0, 1), "vehicle", 0.5)))

      val testID = UUID.randomUUID()
      val registryRef = spawn(domain.TopologicalMapEntity(testID))
      registryRef ! FromMap(map)
      registryRef ! MoveVehicle("vehicle", NetworkLocation((0, 1), 1.0))

      // when
      val stopProbe = testKit.createTestProbe[Confirmation]
      registryRef ! Stop(Some(stopProbe.ref))
      stopProbe.expectMessage(Done)

      val restartedRegistryRef = spawn(domain.TopologicalMapEntity(testID))

      // then
      val stateProbe = testKit.createTestProbe[CurrentMap]
      restartedRegistryRef ! GetMap(stateProbe.ref)

      val expectedTopologicalMap = map.moveVehicle("vehicle", NetworkLocation((0, 1), 1.0))
      stateProbe.expectMessage(CurrentMap(Some(expectedTopologicalMap)))
    }
  }

}
