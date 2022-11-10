# Decision

Our APIs will undergo the following refactoring:

| Item                         | Description                                                                               | Proposed Change:                                                                                                                |
|------------------------------|-------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| Data Management API          | API to handle runtime objects, such as `Assets`, `Policies`, etc.                         | Change name/moniker to "Management API", change default path from `/api/v1/data` -> `/api/v1/management`                        |
| Data Plane Control API       | exposed by the Data Plane to initiate transfers and obtain information about them         | provide a default path at `/api/v1/control`                                                                                     |
| Data Plane Public API        | exposes a generic set of endpoints publicly, which are then gatewayed to an internal API  |                                                                                                                                 |
| Transfer Process Control API | exposed by the control plane to allow the Data Plane to communicate updates in a transfer | Change name/moniker to "Control API", change default path from `/api/v1/controlplane` to `/api/v1/control`                      |
| Federated catalog API        | API that gives access to the locally cached Federated Catalog                             | rename to "Federated Catalog API", register with dedicated context alias and provide default path at `/api/v1/federatedcatalog` |

# Rationale

Some APIs are named in a confusing way, e.g. the "Data Management API" has nothing to do with data, but it's (default)
path is `/api/v1/data`.
Similarly, there are APIs such as the control APIs for data plane and control plane, that are named inconsistently, and
their (default) paths are also inconsistent.

