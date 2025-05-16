package com.urban.mobility.travelling.application

import com.urban.mobility.topology.domain.model.{Edge, NetworkLocation, Node}
import com.urban.mobility.travelling.domain.Type.AverageTime

object EATStatistic {

  def compute(vehicle: NetworkLocation,
              averageTimes: Map[Edge.ID, Option[AverageTime]],
              allPathsBetweenAnyTwoNodes: Map[Node.ID, Map[Node.ID, List[List[Edge.ID]]]]): Map[Node.ID, AverageTime] = {

    val NetworkLocation(currentEdge, currentPosition) = vehicle

    averageTimes(currentEdge) match {
      case Some(edgeAverageTime) =>
        val allPathsFromNextNodeToAnyOther: Option[Map[Node.ID, List[List[Edge.ID]]]] =
          allPathsBetweenAnyTwoNodes.get(currentEdge._2)

        val eat_0 = computeInitialEAT(edgeAverageTime, currentPosition)

        allPathsFromNextNodeToAnyOther
          .map(allPaths => {
            val eatMap = allPaths
              .map { case (other, paths) => other ->
                paths
                  .map(path => computeEAT(path, averageTimes, eat_0))
                  .filterNot(_.isEmpty)
                  .map(_.get)
                  .min
              }
            eatMap + (currentEdge._2 -> eat_0)
          })
          .getOrElse(Map(currentEdge._2 -> eat_0))

      case _ => Map()
    }
  }

  private def computeInitialEAT(edgeAverageTime: AverageTime, currentPosition: Double): AverageTime =
    edgeAverageTime * (1.0 - currentPosition)

  def computeEAT(edgeList: List[Edge.ID],
                 averageTimes: Map[Edge.ID, Option[AverageTime]],
                 initialTime: AverageTime): Option[AverageTime] =

    if (!edgeList.exists(averageTimes(_).isEmpty))
      Some(edgeList.foldLeft(initialTime)((avt, edge) => avt + averageTimes(edge).getOrElse(0.0)))
    else
      None
}
