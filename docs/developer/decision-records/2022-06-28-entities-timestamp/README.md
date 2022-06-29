# Entities timestamp

## Decision

An entity represents any singular, identifiable and separate object inside an application.

The decision related based on the definition explained above is to add a timestamp to the EDC entities in their state of
creation. These Entities are `ContractAgreement`, `ContractDefinition`, `PolicyDefinition`, and `Asset`.

The entity classes `ContractNegotiation` and `TransferProcess` will extend the `StatefulEntity` class. The other
entities will extend the `Entity` class. the `Entity` class will take the attributes `id` and `createdTimestamp` from
`statefulEntity` and it will become its superclass.

## Rationale

As it was already done with the entities which extended the `StatefulEntity` class, where the entities can store the
timestamp of their creation and also the last change of state timestamp.

The idea of this change is to provide timestamps for creation of entities. This approach is similar to the entities that
are extended from the class `StatefulEntity`

## Approach

Since some entities once are created, do not change their state, it is possible to differentiate them from the ones that
do it. This is the reason why an abstraction called `Entity` was extracted from the class `StatefulEntity`.

The timestamp will be

As it was explained in the **Decision** section, a class `Entity` will be the superclass of the class `StatefulEntity`.
This class will contain and `id` and a `timestamp` as attributes when the entity was created. The entity classes `Asset`
,
`ContractDefinition`, `PolicyDefinition` and `ContractAgreement` will extend this class. Their SQL schemas will also be
modified, so that the timestamps can also be stored persistently.

in the `Asset` class the attribute property_id (which is currently stored in the hashmap attribute properties) will be
replaced with the id from the extended class `Entity`. With this change we can also refactor the store for the class
`Asset`, just like it was done with the classes `ContractNegotiation` and `TransferProcess`