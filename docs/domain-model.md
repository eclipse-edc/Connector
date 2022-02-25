# Domain Model

![domain-model](diagrams/domain-model.png)
> The shown picture illustrates only a generic view of the Domain Model and is not intended to show all aspects of the project.

## Asset

The data (Databases, Files, Cache Information, etc) to be published and shared between organizations is represented as
an asset. Every asset owns its [Data address](#data-address).

## Data Address

Description of the location where the asset data is or will be located.

## Contract

A contract always contains one or more [Assets](#asset), one or more [Policies](#policy). The contract construct is used
to define the arrangement between two parties (consumer and provider). Regarding this arrangement, the contract passes
several stages which are explained in the following:

* ### Contract Definition

  contract definition is a top-down design that associates policies with assets. This contract definition contains
  access policies, contract policies, and an asset selector which links the contract to one or more assets. Access
  policies are non-public requirements for accessing the contract offers. Contract policies are also requirements for
  accessing the contract offers. In addition, the consumer must follow these policies when accessing the assets.

* ### Contract Offer

  The contract offer is a dynamic representation of the [contract definition](#contract-definition)
  for a specific consumer and will be used for the contract negotiation. This contract offers are not persistent and
  will be regenerated on every request. The provider EDC will only generate contract offers for the contract definitions
  where the inquiring organizations privileges satisfy the policies established in the contract definitions access
  policy. One contract offer is always related to only one asset of the contract definition (e. g. if the contract
  definition is related to 3 assets, the consumer will get 3 separate contract offers). In addition, the contract offer
  contains a policy which is the contract policy.

* ### Contract Negotiation

  Is the process when an organization, represented with a connector as a consumer, asks for access to a certain asset
  and supplies all the requirements specified in the contract offer. Some states of the contract negotiation are the
  requesting of an asset, the offers asked in the negotiation, the approval or the declining of it.

* ### Contract Agreement

  agreed usage between data provider and data consumer after a contract negotiation. It points to a contract offer and
  can have expiry or cancellation date.

## Policy

Contract policies represent permitted and prohibited actions over a certain asset. These actions can be limited further
by constraints (temporal or spatial) and duties (e.g. deletion of the data after 30 days). Further information is
provided in a separate [section](Policies.md).

## Data Request

Data Request is a representation which creation initialize the data [transfer process](#transfer-process) on the
consumer side. It contains a reference of the to be transferred [Asset](#asset) and
the [Contract (Agreement)](#contract-agreement).

## Transfer process

Representation of the data transfer. Runs through a state machine which defines certain states like provisioning,
transferring, completed.