# Decision

We need to make our API context more clear and well-defined.
We will have 4 API contexts:

#### Management API

All APIs that are intended to handle runtime objects, such as `Assets`, `Policies`, but also operations that are meant
to be done by a third-party client, as backend services or UIs.
This context will expose the controllers defined by the following modules:

- `data-management-api`
- `data-plane-selector`
- `federated-catalog`
- `http-provisioner`
- `observability-api`

Network context: intra-organization

*Default path*: `/api/v1/management`

#### Control API

Refers to APIs that are used by the connector internally facilitate communication between various components.
This context will expose the controllers defined by modules as `data-plane-api`, `control-plane-api` and `validation`.

Network context: private

*Default path*: `/api/v1/control`

#### Protocol API

This API is used to enable the dataspace protocol communication between connectors of different participants.

Network context: public

*Default path*: `/api/v1/ids`

_Note that at the time of this writing ((Nov 2022) IDS requires the path to end with `/data`. That will be added by the
multipart controller, so the resulting path will be `/api/v1/ids/data`._

#### Public API

This refers to endpoints that are used to facilitate data transfers between the data planes of different participant.

Network context: public

# Rationale

Some APIs are named in a confusing way, e.g. the "Data Management API" has nothing to do with data, but it's (default)
path is `/api/v1/data`.
Similarly, there are APIs such as the control APIs for data plane and control plane, that are named inconsistently, and
their (default) paths are also inconsistent.
It's also difficult to configure the endpoints from a network perspective because they are many and the scope is not
clear. With the new definition, it will become clear which contexts need to be exposed on which network level.