package com.urban.mobility.bootstrap

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.urban.mobility.bootstrap.config.BootConfiguration
import com.urban.mobility.tracking.infra.web.rest.TopologicalMapRoute
import com.urban.mobility.tracking.infra.web.ws.VehicleLocationRoute
import com.urban.mobility.travelling.infra.web.rest.{ArrivalTimeRoute, TravelDistanceRoute, TravelTimeRoute}

import scala.concurrent.ExecutionContext

object WebServer extends App {

  val rootBehavior = Behaviors.setup[Nothing] { context =>

    implicit val classicActorSystem: akka.actor.ActorSystem = context.system.toClassic
    implicit val executor: ExecutionContext = context.system.executionContext

    val BootConfiguration(topologyService, travelService) = BootConfiguration()(context, executor)

    val route: Route =
      pathEndOrSingleSlash {
        get {
          complete("Welcome to web server")
        }
      } ~
        TopologicalMapRoute(topologyService) ~
        VehicleLocationRoute.pullBased(topologyService)(context.system) ~
        VehicleLocationRoute.pushBased(topologyService)(context.system) ~
        TravelDistanceRoute(travelService) ~
        TravelTimeRoute(travelService) ~
        ArrivalTimeRoute(travelService)

    Http().bindAndHandle(route, "localhost", 8080)

    Behaviors.empty
  }
  ActorSystem[Nothing](rootBehavior, "UserGuardian")
}
