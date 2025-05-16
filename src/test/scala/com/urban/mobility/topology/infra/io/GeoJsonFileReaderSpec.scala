package com.urban.mobility.topology.infra.io

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class GeoJsonFileReaderSpec extends AnyFlatSpec with Matchers {

  "reader" must "list the segments out of the not bidirectional linestring for the one feature described in the given test file" in {
    // given
    // when
    val segments = GeoJsonFileReader("/test-nobidir.geojson")
    // then
    segments.size must be(3)
    segments must be(List(
      ((0.0, 0.0), (10.0, 0.0)),
      ((10.0, 0.0), (10.0, 10.0)),
      ((10.0, 10.0), (0.0, 0.0))))
  }

  "reader" must "list the segments out of the bidirectional linestring for the one feature described in the given test file" in {
    // given
    // when
    val segments = GeoJsonFileReader("/test-bidir.geojson")
    // then
    segments.size must be(2)
    segments must be(List(
      ((0.0, 0.0), (10.0, 0.0)),
      ((10.0, 0.0), (0.0, 0.0))))
  }
}
