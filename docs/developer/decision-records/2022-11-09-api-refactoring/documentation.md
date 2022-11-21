# Decision

We will move away from one comprehensive `openapi.yaml` file and move towards creating separate OpenAPI specifications
for all of our APIs and publish them to SwaggerHub separately.

# Rationale

EDC offers several APIs, which differ in their intended usage scenario, their visibility and likely their
authentication mechanism. For example, the Management API is intended for consumption by external clients, whereas the
control API is intended to facilitate communication between data plane and control plane. It is therefor likely that the
respective documentation (i.e. Swagger) will be consumed by different user groups.

Adding to this is the fact that we're publishing different APIs under different ports and context paths, which makes it
difficult (if not impossible) to provide separate `@Server` sections for each them. That means, that _all_ APIs would
effectively hit a server at the same port and base path. There is the `@ServerVariable` annotation, but again, that
would affect all endpoints in a single YAML file equally.

# Approach

Every module that contains an API, i.e. a controller class annotated with Swagger annotations, produces a YAML file that
contains documentation about its controllers. We'll have to find a way to group together several of these "partial
specs". For example, the Management API is made up of the Asset API, the Policy API, etc., so we need to merge them into
a `management-api.yaml` file.

One possible approach to achieve this would be to either utilize the `@Tags` and `@Tag` annotations, or come up with
custom annotations, and extend or wrap around the `resolve` Gradle task (from the Swagger plugin), so that input
files/directories are grouped together based on these annotations.

_Note: writing a custom Gradle plugin/task for this could become necessary_

For example, every controller that is annotated with `@ApiGroup(name="management-api")` would produce
a partial spec, i.e. a `<modulename>.yaml` file, which contains the controller's documentation and that - during the
merging would contribute to a `management-api.yaml` file.

Technically speaking there would have to be a subfolder called e.g. `management-api`, into which all partial spec files
are put. Then, the `mergeOpenApiFiles` task would have to be invoked for every one of these subfolders, thus merging its
contents and ultimately producing the (merged) `management-api.yaml`.

Similarly, when merging the "partial specs" together using the `openApiMerger`, we'd get several API specs,
e.g. `management-api.yaml`, `control-api.yaml`, etc.

Ultimately, all of these will get published as separate APIs to SwaggerHub.