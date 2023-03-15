# Allowing updates on user-created entities

## Decision

We will implement additional endpoints in the DataManagementAPI to allow updates on the following entities:

- `Asset`
- `DataAddress`
- `ContractDefinition`
- `PolicyDefinition`

## Rationale

EDC should not be opinionated regarding the mutability or immutability of these business objects, unless there is
technical reason for it. It is up to the client application's business rules to decide whether deleting or modifying
certain business objects is allowed or not, given certain circumstances.

Oftentimes client applications may want to be able to correct formal errors such as typos, or even technical errors,
such as mistyped URLs or missing parameters. Since there is no way to distinguish one from the other, the only solution
is to allow all updates.

## Approach

For each entity there will be an additional REST endpoint with the following properties:

- `PUT` is used as HTTP verb, i.e. `PUT /{object_type_path}/{id}`
- the endpoint accepts _the entire_ business object in the request body, effectively replacing it
- if the object in question does not yet exist, a new one should be created
- `PUT` operations are idempotent, so multiple identical requests produce the same result
- if the object didn't exist before and was newly created, return HTTP 201, otherwise return HTTP
  200 (see [reference](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status#successful_responses))

A brief overview of these rules is given [here](https://restfulapi.net/rest-put-vs-post/).

New code paths should be added from the service layer down. Analogous to the `create` and `delete` use case, an event
should be emitted by the service layer via the observable/listener interfaces.

## Disclaimer

> Whether updating an object is allowed from a business or even legal perspective in any given circumstance must
> ultimately be decided by the client application. Any technical measures to prevent access to these endpoints (API
> gateways, proxies, or even specialized implementations of the `AuthenticationService`) are thus up to the client
> application as well.

## Further considerations

If both being able to update _and_ ensuring immutability are required, encoding this in a policy may be an option.
For example making sure that an `Asset` is not modified after a contract for it has been negotiated, this rule could be
encoded into a policy. A hash of the original Asset is stored on the contract, and upon policy evaluation the hash is
re-computed and compared to the original one.

Many databases offer ways to create audit tables, either through built-in
features ([CosmosDB](https://learn.microsoft.com/en-us/azure/cosmos-db/audit-control-plane-logs)) or through manual
triggers ([PostgreSQL](https://wiki.postgresql.org/wiki/Audit_trigger)). These are explicitly out-of-scope for this
implementation and could be configured externally.