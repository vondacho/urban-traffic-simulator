package com.urban.mobility.travelling.infra.web.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait TravelStatisticsJson extends SprayJsonSupport with DefaultJsonProtocol {
}
