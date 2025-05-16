package com.urban.mobility.shared.util

import com.vividsolutions.jts.geom.Coordinate

object Geometry {
  val euclidianDistance: (Coordinate, Coordinate) => Double = (from, to) => from.distance(to)
}

