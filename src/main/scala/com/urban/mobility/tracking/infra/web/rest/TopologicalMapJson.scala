package com.urban.mobility.tracking.infra.web.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.urban.mobility.topology.domain.model.TopologicalLocation
import spray.json.DefaultJsonProtocol

trait TopologicalMapJson extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val topologicalLocationFormat =
    jsonFormat2((segment: (Int, Int), location: Double) => TopologicalLocation(segment, location))
  implicit val topologicalMapFormat = jsonFormat3(TopologicalMapDto)
}
