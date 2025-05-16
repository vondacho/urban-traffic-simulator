package com.urban.mobility.topology.domain.model

import com.vividsolutions.jts.geom.{Coordinate, LineSegment}

object Node {
  type ID = Int
}

object Edge {
  type ID = (Node.ID, Node.ID)
}

/**
 * A network map encode the representation of the road network together with point of interests.
 **/
case class NetworkMap(
                       // Nodes with their coordinate.
                       // Int        - Node identifier.
                       // Coordinate - Location of the node.
                       nodes: Map[Node.ID, Coordinate],
                       // Directed edges connection nodes.
                       // Int    - Source station identitifer
                       // Int    - Target station identitifer
                       edges: List[Edge.ID]
                     ) {

  val allPathsToAnyNode: Map[Node.ID, Map[Node.ID, List[List[Edge.ID]]]] =
    nodes.map(id => id._1 -> allPathsFromOneNodeToAnother(id._1))

  def edgesFromNode(node: Node.ID): List[Edge.ID] = edges.filter(_._1 == node)

  def pathsFromTo(from: Node.ID, to: Node.ID): List[List[Edge.ID]] = allPathsToAnyNode(from)(to)

  def toLocation(absolute: Coordinate): Option[NetworkLocation] = {
    val candidateList: List[(Edge.ID, Double)] =
      findEdgesByPosition(absolute).map(candidate =>
        (candidate, edgeFraction(toCoordinates(candidate, nodes), absolute)))

    if (candidateList.isEmpty) None
    else {
      val bestCandidate = candidateList.reduceLeft((a, b) => if (a._2 < b._2) a else b)
      Some(NetworkLocation(bestCandidate._1, bestCandidate._2))
    }
  }

  private def findEdgesByPosition(position: Coordinate): List[Edge.ID] =
    edges.filter(edge => isOnEdge(toCoordinates(edge, nodes), position))

  private def edgeFraction(edge: (Coordinate, Coordinate), position: Coordinate): Double =
    new LineSegment(edge._1, edge._2).segmentFraction(position)

  private def isOnEdge(edge: (Coordinate, Coordinate), position: Coordinate): Boolean =
    new LineSegment(edge._1, edge._2).distance(position) < 0.05

  private def toCoordinates(edge: Edge.ID, nodes: Map[Node.ID, Coordinate]): (Coordinate, Coordinate) =
    (nodes(edge._1), nodes(edge._2))


  private def allPathsFromOneNodeToAnother(node: Node.ID): Map[Node.ID, List[List[Edge.ID]]] = {
    def allPathsBetween(from: Node.ID,
                        to: Node.ID,
                        visited: List[Node.ID],
                        curr: List[Edge.ID]): List[List[Edge.ID]] = {

      if (from == to) {
        List(curr.reverse)
      } else {
        if (!visited.contains(from)) {
          val nextEdges = edgesFromNode(from)
          if (nextEdges.isEmpty) Nil
          else nextEdges.flatMap(edge =>
            allPathsBetween(edge._2, to, from :: visited, edge :: curr).filterNot(_.isEmpty))
        }
        else Nil
      }
    }

    nodes.keys
      .filterNot(_ == node)
      .map(to => to -> allPathsBetween(node, to, Nil, Nil))
      .toMap
  }

}

object NetworkMap {

  def apply(segments: List[((Double, Double), (Double, Double))]): NetworkMap = {
    val nodeSet = (segments.map(_._1) ++ segments.map(_._2)).distinct
    val nodeMapByCoordinate = nodeSet.zipWithIndex.toMap
    val nodeMapByIndex = nodeSet.zipWithIndex.foldLeft(Map[Node.ID, Coordinate]()) {
      (acc, valueKey) => acc + (valueKey._2 -> new Coordinate(valueKey._1._1, valueKey._1._2))
    }
    val edgeSet = segments.map(s => (nodeMapByCoordinate(s._1), nodeMapByCoordinate(s._2)))

    NetworkMap(nodeMapByIndex, edgeSet)
  }

  def empty = NetworkMap(Map(), List())
}

case class NetworkLocation(edge: Edge.ID, position: Double)
