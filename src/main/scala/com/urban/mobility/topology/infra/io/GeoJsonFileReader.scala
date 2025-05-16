package com.urban.mobility.topology.infra.io

import com.urban.mobility.topology.infra.io.GeoJsonProtocol._
import spray.json._

import scala.io.Source

object GeoJsonFileReader {

  import LineStringReader._

  def apply(filename: String): List[((Double, Double), (Double, Double))] =
    readFileContent(filename)
      .parseJson.convertTo[FeatureCollection]
      .map(feature => (feature.geometry.coordinates.toList.map(point => (point(0), point(1))), feature.bidirectional))
      .toList
      .flatMap { case (linestring, bidirectional) => toSegments(linestring, bidirectional) }

  private def readFileContent(filename: String): String = {
    val fileStream = getClass.getResourceAsStream(filename)
    val lines = Source.fromInputStream(fileStream).getLines
    val content = new StringBuilder
    lines.foreach(line => content.append(line))
    content.mkString
  }
}

object LineStringReader {

  val UNIRECTIONAL_LINESTRING = 0
  val BIRECTIONAL_LINESTRING = 1

  def toSegments(lineString: List[(Double, Double)], bidir: Int = UNIRECTIONAL_LINESTRING): List[((Double, Double), (Double, Double))] = {
    val coordinates = lineString.map(point => (point._1, point._2))

    if (bidir == BIRECTIONAL_LINESTRING) {
      val coordinatesReversed = coordinates.reverse
      (coordinates zip coordinates.tail) ++ (coordinatesReversed zip coordinatesReversed.tail)
    }
    else {
      coordinates zip coordinates.tail
    }
  }
}
