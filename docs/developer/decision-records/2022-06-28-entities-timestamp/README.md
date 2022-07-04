# Entities timestamp

## Decision

An entity represents any singular, identifiable and separate object inside an application.

The decision based on the definition explained above is to add a timestamp to the EDC entities in their state of
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

The class Entity will contain 2 attributes

```java

public abstract class Entity<T extends Entity<T>> {

    protected String id;
    protected long createdTimestamp;

    protected Entity() {
    }

    ...
}    
```

The class StatefulEntity will extend Entity

```java
public abstract class StatefulEntity<T extends StatefulEntity<T>> extends Entity implements TraceCarrier {

    ...
}

```

The timestamp will be measured as epoch seconds, using systemDefault (UTC) as ZoneId.

```java

Clock clock=Clock.systemDefaultZone();
        var sample=clock.instant();
        var sampleSec=clock.instant().getEpochSecond();
```

As it was explained in the **Decision** section, a class `Entity` will be the superclass of the class `StatefulEntity`.
This class will contain and `id` and a `timestamp` as attributes when the entity was created. The entity classes `Asset`
, `ContractDefinition`, `PolicyDefinition` and `ContractAgreement` will extend this class. Their SQL schemas will also
be modified, so that the timestamps can also be stored persistently.

With the adding of the attribute `createdTimestamp` the module `ContractDefinitionStore`, and its classes would be
modified like the following:

`Schema.sql`

```sql

CREATE TABLE IF NOT EXISTS edc_contract_definitions
        (
        contract_definition_id VARCHAR NOT NULL,
        access_policy_id VARCHAR NOT NULL,
        contract_policy_id VARCHAR NOT NULL,
        created_timestamp VARCHAR NOT NULL,
        selector_expression JSON NOT NULL,
        PRIMARY KEY(contract_definition_id)
        );
```

Class `contractDefinitionStatements`

```java

public interface ContractDefinitionStatements {
    ...

    default String getCreatedTimestamp() {
        return "created_timestamp";
    }
    ...

    String getFindByTimestamp();
}

```

`class BaseSqlDialectStatements`

```java

public class BaseSqlDialectStatements implements ContractDefinitionStatements {
...

    @Override
    public String getFindByTimestamp() {
        return format("SELECT * FROM %s WHERE %s > ? and %s < ?", getContractDefinitionTable(), getCreatedTimestamp());
    }

    @Override
    public String getInsertTemplate() {
        return format("INSERT INTO %s (%s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?%s)",
                getContractDefinitionTable(),
                getIdColumn(),
                getAccessPolicyIdColumn(),
                getContractPolicyIdColumn(),
                getCreatedTimestamp(),
                getSelectorExpressionColumn(),
                getFormatAsJsonOperator());
    }    
    ...
}
```

in the `Asset` class the attribute property_id(which is currently stored in the hashmap attribute properties)will be
replaced with the id from the extended class `Entity`.With this change we can also refactor the store for the class
`Asset`,just like it was done with the classes `ContractNegotiation` and `TransferProcess.

        ```java

public class Asset extends Entity {

    public static final String PROPERTY_NAME = "asset:prop:name";
    public static final String PROPERTY_DESCRIPTION = "asset:prop:description";
    public static final String PROPERTY_VERSION = "asset:prop:version";
    public static final String PROPERTY_CONTENT_TYPE = "asset:prop:contenttype";

    private Map<String, Object> properties;
    ...

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder<B extends Builder<B>> {
        protected final Asset asset;
        private String id;
        private long createdTimestamp;

        protected Builder(Asset asset) {
            this.asset = asset;
            id = UUID.randomUUID().toString();
            createdTimestamp = asset.getCreatedTimestamp();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new Asset());
        }

        public B id(String id) {
            this.id = id;
            return (B) this;
        }

... } }

```