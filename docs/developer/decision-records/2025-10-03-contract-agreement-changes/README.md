# Contract Agreement Changes

## Decision

We will decouple the `id` of the `ContractAgreement` from the identifier of the contract shared between the `consumer`
and `provider`.

## Rationale

Currently, the `id` of the `ContractAgreement` is used as the identifier of the contract between the `consumer` and
`provider`. It is also referenced in the `ContractNegotiation` as `contractAgreementId` and in the `TransferProcess` as
`contractId`. This means that when a new contract is negotiated between two `ParticipantContext` instances running in
the same connector, a new `ContractAgreement` is created with a new `id`, which is then shared between the `consumer`
and `provider`. This may lead to issues and having only a `participantContextId` in the `ContractAgreement` would not be
sufficient to resolve this case.

## Approach

We will introduce a new field in the `ContractAgreement` called `agreementId`, which will be used as the identifier of
the contract shared between the `consumer` and `provider`. To maintain backward compatibility, the `id` field will
remain as the unique identifier of the `ContractAgreement` entity, used as a foreign key in the `ContractNegotiation`
and `TransferProcess` entities, as well as in the management APIs.

The new `agreementId` field will act as the `id` in the protocol layer between the `consumer` and `provider`. The
combination of `agreementId` and `participantContextId` should also uniquely identify a `ContractAgreement` within a
connector.

### Migration

Migrating to the new approach in a SQL store implementation or equivalent will require updating the database schema by
adding a new `agreementId` field to the `ContractAgreement` and assigning the value of the existing `id` field to the
new
`agreementId` field for all existing `ContractAgreement` records.

Additionally, the `participantContextId` field should be added as part of bringing the `ParticipantContext` into all
entities.

### Impact

The management APIs will continue to use the `id` field as the unique identifier of the `ContractAgreement` entity,
the only difference is that the `id` field will no longer represent the identifier of the
contract shared between parties. Instead, the new `agreementId` field will be used for that purpose, and it will be
added to the `ContractAgreement` management API.
