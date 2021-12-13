# Connector Observability

### `/check/health` endpoint

returns information about the system status. This is primarily intended to be used
in [`HEALTHCHECK` instructions in Docker files](https://docs.docker.com/engine/reference/builder/#healthcheck). Here,
this endpoint always returns HTTP 200 indicating that the system is healthy as soon as this endpoint is reachable.

### `/check/liveness` endpoint

From the [Kubernetes docs](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#container-probes):
> indicates whether the container is running.

Basically the same semantics as the `/health` endpoint. We can assume, that in the event that the connector crashes, all
REST endpoints become unavailable.

### `/check/readiness` endpoint

From the [Kubernetes docs](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#container-probes):
> Indicates whether the container is ready to respond to requests.

Thus, the readiness endpoint must return a value indicating success _only after_ all extensions that contribute an API
have started successfully. Note, that other subsystems like the catalog crawlers might not yet be completed at that
point, but technically the connector is "ready to respond to requests", even if they might not produce the desired - or
even a sensible - outcome. Catalog queries may produce an empty or incomplete result until crawlers have completed.

### `/check/startup` endpoint

From the [Kubernetes docs](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#container-probes):
> Indicates whether the application within the container is started

This is very similar to the `/check/readiness` endpoint, but a connector may be able to respond to requests as soon as
all extensions contributing APIs (e.g. REST controllers) have been registered, thus being `ready`,, whereas the startup
is only completed after _all_ extensions have started. This can only be determined by the runtime. Again, parallel
systems like crawlers will **not affect** system readiness.
