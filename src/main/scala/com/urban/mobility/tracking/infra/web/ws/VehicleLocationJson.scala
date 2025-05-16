package com.urban.mobility.tracking.infra.web.ws

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.urban.mobility.topology.domain.model.{NetworkLocation, TopologicalLocation}
import spray.json.DefaultJsonProtocol

trait VehicleLocationJson extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val topologicalLocationFormat =
    jsonFormat2((segment: (Int, Int), location: Double) => TopologicalLocation(segment, location))
  implicit val networkLocationFormat =
    jsonFormat2(NetworkLocation)
}
