# Entities timestamp

## Decision

An entity represents any singular, identifiable and separate object inside an application.

The decision based on the definition explained above is to add a timestamp to the EDC business objects in their state of
creation. These Entities are `ContractAgreement`, `ContractDefinition`, `PolicyDefinition`, and `Asset`. Since they do
not change of state as the classes `ContractNegotiation` and `TransferProcess` there is no need to add a change of state
timestamp.

## Rationale

The idea of this change is to add helpful information to the metadata of these business objects, giving for example the
possibility of searching for them based on their date and time of creation.

## Approach

The classes `Asset` , `ContractDefinition`, `PolicyDefinition` and `ContractAgreement` will contain each one an
attribute called `createdTimestamp`. Their SQL schemas will also be modified, so that the timestamps can also be stored
persistently.

The timestamp at the time of creation of the Entity, will be valued with the method `Clock.millis()`

With the adding of the attribute `createdTimestamp` the module `ContractDefinitionStore`, and its classes would be
modified like the following:

### Contract Definition

Class `ContractDefinition`

```Java
public class ContractDefinition {
...
    private long createdTimestamp;

    ...

    @NotNull
    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    ...

    public static class Builder {
    
    ...

        public Builder createdTimestamp(long createdTimestamp) {
            definition.createdTimestamp = createdTimestamp;
            return this;
        }

        ...

        public ContractDefinition build() {
            Objects.requireNonNull(definition.id);
            Objects.requireNonNull(definition.accessPolicyId);
            Objects.requireNonNull(definition.contractPolicyId);
            Objects.requireNonNull(definition.selectorExpression);
            Objects.requireNonNull(definition.createdTimestamp);
            return definition;
        }
    ...
    }
```

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