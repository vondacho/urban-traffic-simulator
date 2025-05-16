package com.urban.mobility.topology.infra.io

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class LinestringReaderSpec extends AnyFlatSpec with Matchers {

  import LineStringReader._

  "reader" must "list the segments out of the unidirectional linestring" in {
    // given
    val linestring = List((0.0, 0.0), (10.0, 0.0), (10.0, 10.0), (0.0, 0.0))
    // when
    val segments = toSegments(linestring, UNIRECTIONAL_LINESTRING)
    // then
    segments.size must be(3)
    segments must be(List(
      ((0.0, 0.0), (10.0, 0.0)),
      ((10.0, 0.0), (10.0, 10.0)),
      ((10.0, 10.0), (0.0, 0.0))))
  }

  "reader" must "list the segments out of the bidirectional linestring" in {
    // given
    val linestring = List((0.0, 0.0), (10.0, 0.0))
    // when
    val segments = toSegments(linestring, BIRECTIONAL_LINESTRING)
    // then
    segments.size must be(2)
    segments must be(List(
      ((0.0, 0.0), (10.0, 0.0)),
      ((10.0, 0.0), (0.0, 0.0))))
  }
}
