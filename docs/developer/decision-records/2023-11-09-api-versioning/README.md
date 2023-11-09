# Consistent Versioning of external APIs

## Decision

Going forward, all our external APIs will be versioned consistently across all endpoints and controllers. This means,
that the
Management API (to name a prominent example) will encode the same version number consistently across all its existing service endpoint URLs, within a major version.

## Rationale

Currently, every module that contributes an API can have its own versioning model. For example, at the time of writing
some APIs are available under `/v2/...`, some are available under `/v3/...` and some without a version specifier.

This can be confusing to developers who want to consume the API, because they not only need different base paths for
every API, they also need to re-check every endpoint when a new version of EDC is released, because every API can change
individually.

Therefore, we will use consistent versioning across _all_ client-facing APIs. At the time of writing, this includes just
the Management API.

## Approach

### Versioning scheme

Following the general semantics of SemVer, we will distinguish between a breaking change (major version bump) and
non-breaking and patch changes (minor/patch version bump). The major version will be represented in the URL:

```
https://some.host.com/api/management/v4/...
```

This means, client code only needs to be updated on major version changes of an API.

> Only major versions are represented in the URL

> Minor or patch updates will **not** be represented in the URL, and we will **not** host multiple minor/patch versions
> of an API at any given time.

Consequently, when multiple major versions of an API are available, every versioned URL (e.g. `/v3/assets`) effectively
contains the _latest_ version, so for instance `/v3/assets` would effectively point to version `3.1.4` of the Management
API.

> The same exact version is used for the entire Management API, we do **not** version independent modules or controllers

> The version of an API is independent of the version of EDC.

### Delegation mechanism for controllers

Not every endpoint changes from version to version. We will have to make the _same_ controller available
under _multiple_ URL paths as we do not want to duplicate controllers. There are several potential ways we could make
this work:

- Declare the `@Path` as [wildcard](https://docs.oracle.com/javaee/7/api/javax/ws/rs/Path.html)
- implement a custom interceptor, that parses the path and delegates to the correct controller

These are as yet untested and need further investigation with regard to the following aspects:

- do controllers appear as individual endpoints in the OpenAPI documentation?
- is there a significant impact w.r.t. resource consumption and response time?

### Storing and publishing version information

A new endpoint will be available that provides exact information about the version of the API. The path and the response
schema is TBD.

The version of an API must be stored in a file that is available at runtime. Every change to an API must increase that
version property. The `gradle.properties` file would be the obvious choice, however we would have to craft a way to
access that information at runtime.

### Publishing APIs to SwaggerHub

Ultimately, we will move away from publishing our APIs under the EDC version, and will publish them only under their
own version. Every update (major, minor, patch,...) will be published to SwaggerHub.
For this, we will have to update our Swagger-publish workflow quite significantly and read the
API [version information](#storing-and-publishing-version-information).

### Maintenance/deprecation model

As a standard M/O EDC will expose at most two versions of an API at any given time. The older one must be marked
as `deprecated` both in code and in Swagger.

All maintenance (bugfixes, patches, improvements) will **only** be made to the most recent API. Deprecated APIs will
**not** receive any maintenance.
