package com.urban.mobility.travelling.infra.config

import java.util.UUID

import akka.actor.typed.scaladsl.ActorContext
import com.urban.mobility.topology.domain.model.NetworkMap
import com.urban.mobility.travelling.application
import com.urban.mobility.travelling.application.TravelService
import com.urban.mobility.travelling.domain.TravelStatisticsEntity

import scala.concurrent.ExecutionContext

case class TravelConfiguration(service: TravelService)

object TravelConfiguration {

  def apply(network: NetworkMap)(implicit context: ActorContext[Nothing], executor: ExecutionContext): TravelConfiguration = {
    val travelStatisticsID = UUID.randomUUID()
    val travelStatisticsEntity = context.spawn(TravelStatisticsEntity(travelStatisticsID), "travelStatisticsActor")
    TravelConfiguration(application.TravelService(travelStatisticsEntity, network)(context.system, executor))
  }
}


