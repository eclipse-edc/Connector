# Implementation of a Accounts API for the SecureTokenService

## Decision

An Accounts API for the SecureTokenService (STS) will be implemented in a separate module to manage STS accounts.
In addition, the current class `StsClient` will be renamed to `StsAccount`, and `StsClientStore` to `StsAccountStore` to
avoid confusion.

## Rationale

The STS acts as authorization backend for a CredentialService. In EDC, the STS implementation is designed to be deployed
alongside the IdentityHub and acts as Identity Provider (IdP) and for its Presentation API. Although there is
an [automatic way](https://github.com/eclipse-edc/IdentityHub/pull/458) to
synchronize a IdentityHub `ParticipantContext` and an STS account, there still should be a way to administer (create,
update, read and delete) STS accounts at runtime.

In addition, in cases where STS is deployed as standalone component, the IdentityHub may synchronize the
`ParticipantContext` and the STS account using this Accounts API.

The reason for renaming the `StsClient` is that the term "client" is quite overloaded and could mislead developers into
thinking that it is a "client to communicate with the STS token API".

## Approach

Implement a REST API that has the following endpoints:

### `POST /v1/accounts`

Create a new `StsAccount`. Key pairs must be managed _out-of-band_. If no `client_secret` is provided, one will be
generated. This endpoint returns the _entire_ `StsAccount`, including the `client_secret`. Note that there is **no way**
to obtain the `client_secret` again at a later point in time!

### `PUT /v1/accounts/{id}`

Update an existing `StsAccount`. Note that there is a dedicated endpoint for rotating the `client_secret`.

### `GET /v1/accounts/{id}`

Fetch a particular `StsAccount` by ID. The ID is the entity ID (database ID) of the `StsAccount`. The `client_secret` is
**not** returned, only its alias.

### `POST /v1/accounts/query`

Query STS accounts, taking a `QuerySpec` in the request body. The `client_secret` is **not** returned, only its alias.

### `POST /v1/accounts/{id}/secret`

Updates the `client_secret`. The new secret alias - and optionally the new secret - are provided in the request body. If
no secret is provided, one is generated at random. The old client secret is deleted from the Vault and replaced with the
new client secret. If the process fails, manual intervention may be necessary, because Vaults are not transactional
resources. This endpoint returns the current `secret_alias` to make that manual intervention easier.

### `DELETE /v1/accounts/{id}`

Deletes a particular account, removes the `client_secret` from the Vault. Note that this does **not** delete the key
pair.

> The `StsClientServiceImpl` will be extended to accommodate this functionality.

### Handling key-pairs

The STS is designed to be used alongside IdentityHub, either as embedded component, or as standalone runtime. In
either case it should be the IdentityHub that exposes the control surface for handling key-pairs through its Identity
API and its internal automatic synchronization mechanism (`StsAccountProvisioner`). For that reason, the STS Accounts
API does not provide any endpoints to handle key pairs.

_Note that if STS is deployed standalone, IdentityHub could use this Accounts API to handle synchronization via REST._

### Web context

A new web context `"accounts"` will be introduced for the STS Accounts API to provide it its own security realm and
ingress configuration. This is particularly important when STS runs as embedded component in IdentityHub or the
connector controlplane to avoid clashes and provide better separation.

### Security

Out-of-the-box, the STS Accounts API utilizes token-based authentication. This can be extended and
overridden in downstream applications, for example using the delegated authentication feature.

> The STS Accounts API should **never** be exposed to the internet without additional infrastructure!