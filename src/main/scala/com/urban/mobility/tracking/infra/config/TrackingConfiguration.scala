package com.urban.mobility.tracking.infra.config

import java.util.UUID

import akka.actor.typed.scaladsl.ActorContext
import com.urban.mobility.topology.domain.model.NetworkMap
import com.urban.mobility.tracking.application.TrackingService
import com.urban.mobility.tracking.domain
import com.urban.mobility.tracking.domain.TopologicalMapEntity.FromNetworkMap

import scala.concurrent.ExecutionContext

case class TrackingConfiguration(service: TrackingService)

object TrackingConfiguration {

  def apply(network: NetworkMap)
           (implicit context: ActorContext[Nothing], executor: ExecutionContext): TrackingConfiguration = {

    val topologicalMapID = UUID.randomUUID()
    val topologicalMapEntity = context.spawn(domain.TopologicalMapEntity(topologicalMapID), "topologicalMapEntity")
    topologicalMapEntity ! FromNetworkMap(network)

    TrackingConfiguration(TrackingService(topologicalMapEntity, network)(context.system, executor))
  }
}


