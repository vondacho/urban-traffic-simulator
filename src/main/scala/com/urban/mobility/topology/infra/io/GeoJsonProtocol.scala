package com.urban.mobility.topology.infra.io

import spray.json.{DefaultJsonProtocol, JsArray, JsValue, RootJsonReader}

object GeoJsonProtocol extends DefaultJsonProtocol {

  implicit object GeometryJsonReader extends RootJsonReader[Geometry] {
    override def read(value: JsValue): Geometry = {
      Geometry(value.asJsObject.fields("coordinates").convertTo[Array[Array[Double]]])
    }
  }

  implicit object FeatureJsonReader extends RootJsonReader[Feature] {
    override def read(value: JsValue): Feature = {
      Feature(
        value.asJsObject.fields("geometry").convertTo[Geometry],
        value.asJsObject.fields("properties").asJsObject.fields("bidir").convertTo[Int])
    }
  }

  implicit object FeatureArrayReader extends RootJsonReader[Array[Feature]] {
    def read(value: JsValue) = value match {
      case JsArray(elements) => elements.map(_.convertTo[Feature]).toArray[Feature]
      case _ => Array()
    }
  }

  implicit object FeatureCollectionJsonReader extends RootJsonReader[FeatureCollection] {
    override def read(value: JsValue): FeatureCollection = {
      FeatureCollection(value.asJsObject.fields("features").convertTo[Array[Feature]])
    }
  }
}

case class FeatureCollection(features: Array[Feature]) extends IndexedSeq[Feature] {
  def apply(index: Int) = features(index)
  def length = features.length
}

case class Feature(geometry: Geometry, bidirectional: Int)

case class Geometry(coordinates: Array[Array[Double]]) extends IndexedSeq[Array[Double]] {
  def apply(index: Int) = coordinates(index)
  def length = coordinates.length
}


