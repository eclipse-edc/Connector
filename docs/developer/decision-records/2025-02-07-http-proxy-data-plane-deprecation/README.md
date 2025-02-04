# Deprecation of HTTP Proxy in the Data Plane


## Decision

We will deprecate and eventually remove the data-plane proxy feature from the core extensions.

## Rationale

The current proxy functionality, implemented in the data-plane-api module, allows forwarding the request body, path,
query parameters, and HTTP method from the consumer to the provider’s data source endpoint. However, it lacks key proxy
capabilities such as response body forwarding, request throttling, load balancing, and caching—features essential to a
fully-fledged proxy.

Maintaining an HTTP proxy implementation is outside the scope of our team, which is focused on developing and maintaining
Dataspace-related components such as connectors and catalogs. Given the availability of mature proxy solutions, we prefer
to avoid the overhead of maintaining a custom implementation.

To support adopters, we will provide documentation and samples demonstrating how to integrate an external proxy solution.

## Approach

Mark `data-plane-public-api-v2` and all related "proxy" references as deprecated.
Provide a sample implementation demonstrating how to integrate a proxy.
Add relevant documentation to the project website.
