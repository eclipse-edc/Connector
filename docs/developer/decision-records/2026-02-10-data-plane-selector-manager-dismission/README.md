# Data Plane Selector Manager Dismission

## Decision

We will discontinue the Data Plane Selector Manager.

## Rationale

The Data Plane Selector Manager takes care to check the heartbeat of the Data Planes with the goal to exclude those data planes
from the pool of available, that affects not only the actual selection of the data planes but also the catalog contents.

Any data plane unavailability shouldn't result in missing contract offers in the catalog.

Eventual observability should be put at the infrastructure level, and hi-availability could be provided through replication
and use of load balancers.

## Approach

The Data Plane Selector Manager will be removed from the code base.
