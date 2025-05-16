# Urban traffic simulator

This application simulates the movements of vehicles inside an urban network described in a GeoJson format.
It provide several web endpoints for querying vehicles' topological position and 
statistic data about timing and distances per vehicle or segment.

## Solution design

### Domain-Driven Design

The following bounded contexts have been revealed out of the Mobility domain:

* Traffic (vehicle movements simulation)
* Travelling (travel statistics, average time, EAT)
* Tracking (topological map)
* Topology (network)

### API design

#### REST

* GET /mobility/v1/topological-map
* GET /mobility/v1/topological-map/vehicles
* GET /mobility/v1/vehicles/travel-distance
* GET /mobility/v1/vehicles/arrival-times
* GET /mobility/v1/segments/travel-time

#### WebSockets

* GET /mobility/v1/vehicles/location
* GET /mobility/v2/vehicles/location

### Events

* Vehicle movement events are published on the event bus by `VehiclesService`.
* They are consumed by several handlers provided by different bounded contexts.

### Entities

* The travel statistics is hosted by the `TravelStatisticsEntity` persistent entity.
* The topological map is hosted by the `TopologicalMapEntity` persistent entity.
* Both entities expose a message API.
* The persistence of both entities is powered by Event sourcing.

### Clean architecture

* Hexagonal architecture
* Domain at the center,
* Application services around,
* Infrastructure around, composed of Web API routes and configuration components

### Technologies and frameworks

* Scala language
* Akka persistent actors
* Akka HTTP
* Akka event bus
* Akka streams

## Features

### Total travelled distance per vehicles

* The travelled distance per vehicles is maintained adding the delta computed from last and new local positions.
* The distance is calculated using Euclidian coordinates and is not representative.

### Average travel time per edges

* Every vehicle movement event has a timestamp.
* Every segment has an average travel time in seconds
* It should maintain the following data structure:
    * `[ (segment-i, [ (vehicle-j, PT(j,k) = passage-time-k(timestamp-max-k - timestamp-min-k)) ] ]`
    * and then `[ (segment-i, AT(k) = average-of (passage-time-k) on all vehicle-j ]`

### ETA for a given vehicle for all stations
* ETA means Estimated Arriving Time.
* This implementation expresses EAT in seconds.
* We should know, for a given node in the network, the optimal path to all other nodes. This information could
be determined once and delivered with the network map.
* The chosen heuristic for the optimal path minimizes the sum of segment average travel times.
** It is possible to select only the AT statistic dedicated to a given vehicle, instead of the global AT one.
* It should calculate : 
* `ETA (vehicle-i, station-j) = AT(k = segment-current) * (1-position-current) + Sum(AT(segment-k), for every segment until station-j)`
