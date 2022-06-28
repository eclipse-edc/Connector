# Entities timestamp

## Decision

From the class `StatefulEntity`an abstract class called `Entity` will be extracted and become its superclass. Some
entity classes created during the creation of policies and contracts will extend to this class `Entity`.

The classes `TransferProcess` and `ContractNegotiation` will keep extended to the class `StatefulEntity` using all the
inherited attributes from this one.

## Rationale

The idea of this change is to provide a history in time for entities once they are created. This approach is similar to
the entities that are extended from the class 'StatefulEntity' where the entities can store the time of creation and
change of state timestamp.

## Approach

As it was explained in the section Decision, a class `Entity` will be the superclass of the class `StatefulEntity`. This
class will contain as attributes and id and the timestamp when the entity was created. The entity classes `Asset`,
`ContractDefinition`, `PolicyDefinition` and `ContractAgreement` will extend to this class. Their SQL schemas will also
be modified, so that the timestamps can also be stored persistently.

in the class `Asset` the attribute property_id (which is currently stored in the hashmap attribute properties) will be
replaced with the id from the extended class `Entity`. With this change we can also refactor the store for the class
`Asset`, just like it was done with the classes `ContractNegotiation` and `TransferProcess`

The class `Catalog` is not considered as an entity since it is not generated on a request, it does not count with an id,
additionally it is just used as a form of visualization of contract offers.