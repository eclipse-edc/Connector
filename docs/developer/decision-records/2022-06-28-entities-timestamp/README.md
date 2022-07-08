# Entities timestamp

## Decision

An entity represents any singular, identifiable and separate business object inside an application.

The decision based on the definition explained above is to add a timestamp to the EDC entities in their state of
creation. These Entities are `ContractAgreement`, `ContractDefinition`, `PolicyDefinition`, and `Asset`. The classes
`ContractNegotiation` and `TransferProcess` have it already added. timestamp.

## Rationale

The idea of this change is to add helpful information to the metadata of these business objects, giving for example the
possibility of searching for them based on their date and time of creation.

## Approach

The classes `Asset` , `ContractDefinition`, `PolicyDefinition` and `ContractAgreement` will contain each one an
attribute called `createdTimestamp` of type long and will be valued using the millis() method from the `Clock` service.
The SQL schemas related to the entities will also be modified, so that the timestamps can also be stored persistently.

### Asset

The changes related to the entity `Asset` would contain the following:

Class `Asset`

```java
public class Asset {

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
            asset.createdTimestamp = createdTimestamp;
            return this;
        }
        ...

    }
```

File `Schema.sql`

```roomsql
...
-- table: edc_asset
CREATE TABLE IF NOT EXISTS edc_asset
(
    asset_id VARCHAR NOT NULL,
    created_timestamp BIGINT,
    PRIMARY KEY (asset_id)
);
...
```

Class `AssetStatements`

```java
public interface AssetStatements {
    ...

    /**
     * The asset table createdTimestamp column.
     */
    default String getAssetCreatedTimestampColumn() {
        return "created_timestamp";
    }
    ...
}
```

Class `BaseSqlDialectStatements`

```java
public class BaseSqlDialectStatements implements AssetStatements {

    ...

    @Override
    public String getInsertAssetTemplate() {
        return format("INSERT INTO %s (%s, %s) VALUES (?, ?)", getAssetTable(), getAssetIdColumn(), getAssetCreatedTimestampColumn());
    }
    ...
}

```

### Policy Definition

The following changes are related to the entity `PolicyDefinition`:

Class `PolicyDefinition`

```java
public class PolicyDefinition {
    ...
    private long createdTimestamp;

    ...

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
    ...

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        ...

        public Builder createdTimestamp(long createdTimestamp) {
            policyDefinition.createdTimestamp = createdTimestamp;
            return this;
        }
    ...
    }
}
```

File `Schema.sql`

```roomsql
...
CREATE TABLE IF NOT EXISTS edc_policies
(
    policy_id VARCHAR NOT NULL,
    permissions VARCHAR,
    prohibitions VARCHAR,
    duties VARCHAR,
    extensible_properties VARCHAR,
    inherits_from VARCHAR,
    assigner VARCHAR,
    assignee VARCHAR,
    target VARCHAR,
    policy_type VARCHAR NOT NULL,
    created_timestamp BIGINT,
    PRIMARY KEY (policy_id)
);
...
```

Class `SqlPolicyStoreStatements`

```java
public interface SqlPolicyStoreStatements {
    ...

    /**
     * The asset table createdTimestamp column.
     */
    default String getPolicyColumnCreatedTimestamp() {
        return "created_timestamp";
    }
    ...
}
```

class `SqlPolicyDefinitionStore`

```java
public class SqlPolicyDefinitionStore implements PolicyDefinitionStore {
    ...

    private void insert(PolicyDefinition def) {
        transactionContext.execute(() -> {
            try (var connection = getConnection()) {
                var policy = def.getPolicy();
                var id = def.getUid();
                var timestamp = def.getCreatedTimestamp();
                executeQuery(connection, sqlPolicyStoreStatements.getSqlInsertClauseTemplate(),
                        id,
                        timestamp,
                        toJson(policy.getPermissions(), new TypeReference<List<Permission>>() {
                        }),
                        toJson(policy.getProhibitions(), new TypeReference<List<Prohibition>>() {
                        }),
                        toJson(policy.getObligations(), new TypeReference<List<Duty>>() {
                        }),
                        toJson(policy.getExtensibleProperties()),
                        policy.getInheritsFrom(),
                        policy.getAssigner(),
                        policy.getAssignee(),
                        policy.getTarget(),
                        toJson(policy.getType(), new TypeReference<PolicyType>() {
                        }));
            } catch (Exception e) {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        });
    }
    ...
}
```

class `PostgressStatements`

```java
public class PostgressStatements implements SqlPolicyStoreStatements {
    ...

    @Override
    public String getSqlInsertClauseTemplate() {
        return String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                getPolicyTableName(),
                getPolicyColumnId(),
                getPolicyColumnCreatedTimestamp(),
                getPolicyColumnPermissions(),
                getPolicyColumnProhibitions(),
                getPolicyColumnDuties(),
                getPolicyColumnExtensibleProperties(),
                getPolicyColumnInheritsFrom(),
                getPolicyColumnAssigner(),
                getPolicyColumnAssignee(),
                getPolicyColumnTarget(),
                getPolicyColumnPolicyType());
    }
    ...
}

```

### Contract Definition

The changes related to the entity `ContractDefinition` are the following:

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

File `Schema.sql`

```roomsql
CREATE TABLE IF NOT EXISTS edc_contract_definitions
        (
        contract_definition_id VARCHAR NOT NULL,
        access_policy_id VARCHAR NOT NULL,
        contract_policy_id VARCHAR NOT NULL,
        created_timestamp BIGINT,
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
}
```

`class BaseSqlDialectStatements`

```java
public class BaseSqlDialectStatements implements ContractDefinitionStatements {
    ...

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

