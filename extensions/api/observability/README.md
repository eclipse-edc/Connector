# Connector Observability API

This API exposes information about this runtime's health status by accessing the internal `HealthCheckService`. In order
to use it add the following line to your `build.gradle[.kts]`:

```kotlin
implementation(project(":extensions:api:observability"))
// or using maven artifacts in downstream projects:
implementation("org.eclipse.dataspaceconnector:observability-api:${EDC_VERSION}")
```

All endpoints described here have the same simple API:

- they support the `GET` method without query params
- if the health check is successful, i.e. _all_ systems are healthy, HTTP 200 is returned with the `HealthStatus` object
  in the response body
- if one of the systems is unhealthy, an HTTP 503 is returned with the `HealthStatus` object in the response body

## Endpoint description

### `GET /check/health`

returns information about the system status. This is primarily intended to be used
in [`HEALTHCHECK` instructions in Docker files](https://docs.docker.com/engine/reference/builder/#healthcheck). Here,
this endpoint will return HTTP 200 indicating that the system is healthy as soon as the connector has finished starting.
Therefore, the `/check/health` and `/check/startup` endpoints will have the same behaviour.

### `GET /check/liveness`

From the [Kubernetes docs](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#container-probes):
> indicates whether the container is running.

We can assume, that in the event that the connector crashes, all REST endpoints become unavailable.

### `GET /check/readiness`

From the [Kubernetes docs](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#container-probes):
> Indicates whether the container is ready to respond to requests.

Thus, the readiness endpoint must return a value indicating success _only after_ all extensions that contribute an API
have started successfully. Note, that other subsystems like the catalog crawlers might not yet be completed at that
point, but technically the connector is "ready to respond to requests", even if they might not produce the desired - or
even a sensible - outcome. Catalog queries may produce an empty or incomplete result until crawlers have completed.

### `GET /check/startup`

From the [Kubernetes docs](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#container-probes):
> Indicates whether the application within the container is started

This is very similar to the `/check/readiness` endpoint, but a connector may be able to respond to requests as soon as
all extensions contributing APIs (e.g. REST controllers) have been registered, thus being `ready`, whereas the startup
is only completed after _all_ extensions have started. This can only be determined by the runtime. Again, parallel
subsystems like crawlers will **not** affect system startup state.

## Usage in Dockerfiles

Docker supports [health check](https://docs.docker.com/engine/reference/builder/#healthcheck) commands. In order to use
the Observability API for that, simply add this line to your `Dockerfile`:

```dockerfile
FROM openjdk:17-slim-buster

# by default curl is not available, so install it
RUN apt update && apt install curl -y

# HERE you should put all your other instructions, like exposing ports, etc.

# health status is determined by the availability of the /health endpoint
HEALTHCHECK --interval=5s --timeout=5s --retries=10 CMD curl --fail -X GET http://localhost:8181/api/check/health || exit 1

# ENTRYPOINT left out for clarity
```

Adjust the interval, timeout and retries to your convenience. Depending on your base image, you might not need to
install `curl`. However, if it is not present at container runtime, the `HEALTHCHECK` will always fail and the container
will eventually become `unhealthy`.

## Usage in Kubernetes files

Regardless of how you deploy the connector to K8S (perhaps a `deployment`), you can add the following section to your
container definition:

```kubernetes helm
# ...
    spec:
      containers:
        - name: yourContainerName
          imagePullPolicy: IfNotPresent
          image: yourRepo/YourImageName
          ports:
            - name: http
              containerPort: 8181

          readinessProbe:
            initialDelaySeconds: 1
            periodSeconds: 5
            httpGet:
              port: http
              path: /api/check/readiness

          livenessProbe:
            initialDelaySeconds: 3
            periodSeconds: 5
            httpGet:
              port: http
              path: /api/check/liveness

          startupProbe:
            initialDelaySeconds: 1
            periodSeconds: 3
            httpGet:
              port: http
              path: /api/startup
```