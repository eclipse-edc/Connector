# Issuing dynamic queries in SQL data stores

## Introduction

The Management API (DM API) offers a very open and unrestricted query feature: as long as it can be expressed as a
3-tuple in the form

```
<LEFT_OPERAND> <OPERATOR> <RIGHT_OPERAND>
```

it is a valid query. This abstract way of expressing a condition is called a `Criterion`. For example, API clients may
specify a query to obtain a selection of `ContractNegotiation` objects that looks like this:

```
contractAgreement.assetId = myAssetId
```

In practice this would return `ContractNegotiation`s, that have a `ContractAgreement` which references an `Asset` with
the ID `"myAssetId"`.

This means, that the backing `SqlContractNegotiationStore` must be able to do the following things:

1. dynamically map the expression onto it's internal DB schema: a mapping from the properties `contractAgreement` onto
   the column name `contract_agreement` must be made. Furthermore, the store implementation must be able to determine
   how to traverse from `ContractNegotiation` to `ContractAgreement` in the database, which in this case means
   navigation (
   = `JOIN`-ing ) a foreign key.
2. distinguish between a scalar or a collection right-hand operand: operators such as `IN` require the right-hand
   operand to be a list, such as `...WHERE x IN (y1,y1)`
3. substitute the values in the expression with placeholders for prepared statements (=`?`)
4. leverage DB-specific features, like querying a JSON-encoded field, which would only be available in Postgres

The following section explains how all of these are achieved.

## Preliminary: the canonical format

Theoretically it is possible to map every schema onto every other schema, given that they are of equal cardinality. To
achieve that, we introduce the notion of a _canonical format_, which is our internal working schema. In other words,
this is the schema in which objects are represented internally. If in the future we support a wider variety of
translation and transformation paths, everything would have to be transformed into that canonical format first.

In actuality the _canonical format_ of an object is defined by the respective Java class and its field names. For
instance, a query for contract negotiations must be specified using the field names of a `ContractNegotiation` object:

```java
public class ContractNegotiation {
    // ...
    private ContractAgreement contractAgreement;
    // ...
}

public class ContractAgreement {
    // ...
    private final String assetId;
}
```

Consequently, `contractAgreement.assetId` would be valid, whereas `contract_agreement.asset_id` would not. Or, the
left-hand operand looks like as if we were traversing the Java object graph. This is what we call the _canonical format_
. Note the omission of the root object `contractNegotiation`!

## Using `TranslationMapping`

In order to decouple the canonical format and the SQL schema (or any other schema) in terms of naming convention, a
mapping scheme exists to map the canonical model onto the SQL model. The `TranslationMapping` is essentially a
graph-like metamodel of the business objects: every Java class is represented by a mapping class that contains its field
names and the associated SQL column names. The convention is to append `*Mapping` to the class name,
e.g. `PolicyDefinitionMapping`.

### Mapping primitive fields

Primitive fields are stored directly in SQL columns (as opposed to JSON, foreign keys,...). Thus, mapping primitive data
types is trivial: a simple mapping from one onto the other is necessary, for
example, `ContractNegotiation.counterPartyAddress` would be represented in the metamodel as an entry

```java
"counterPartyAddress"->"counterparty_address"
```

When constructing `WHERE/AND` clauses, the canonical property must simply be replaced by the respective SQL column name.

### Mapping complex objects

For fields that are of complex type, such as the `ContractNegotiation.contractAgreement` field, it is necessary to
accommodate this depending on how the EDM is defined. There are two basic variants that we use:

#### Option 1: using foreign keys

In this case, the referenced object is stored in a separate table using a foreign key. Thus, the canonical property
(= `contractAgreement`) is mapped onto the SQL schema using yet another `*Mapping` class. In the given example this
would be the `ContractAgreementMapping`. When resolving a property in the canonical format (`contractAgreement.assetId`)
, this means we must recursively descend into the model graph and resolve the correct SQL expression.

_Note: mapping and translating `one-to-many` relations (= arrays/lists) with foreign keys is not implemented at this
time._

#### Option 2a: encoding the object in JSON

Another way to store complex objects is to encode them in JSON and store them in a `VARCHAR` column. Some databases even
have a special data type and dedicated operators and functions for that. Postgres is one of them. For example,
the `TranferProcess` is stored in a table called `edc_transfer_process`, its `DataAddress` property is encoded in JSON
and stored in a `JSON` field.

For example querying for `TransferProcess` objects: when mapping the filter
expression `contentDataAddress.properties.somekey=somevalue`, the `contentDataAddress` is represented as JSON, therefore
in the `TransferProcessMapping` the `contentDataAddress` field maps to a `JsonFieldMapping`:

```java
public TransferProcessMapping(TransferProcessStoreStatements statements){
        // ...
        add(FIELD_CONTENTDATAADDRESS,new JsonFieldMapping(statements.getContentDataAddressColumn()));
        // ...
        }
```

which would then get translated to:

```sql
SELECT *
FROM edc_transfer_process
-- omit LEFT OUTER JOIN for readability
WHERE content_data_address -> 'properties' ->> 'key' = 'value'
```

_Note this statement is only valid in the Postgres dialect! Other SQL databases may or may not have similar
possibilities._

#### Option 2b: encoding lists/arrays in JSON

Like accessing objects, accessing lists/arrays of objects is possible using special JSON operators. In this case the
special Postgres function `json_array_elements()` is used. Please refer to
the [official documentation](https://www.postgresql.org/docs/9.5/functions-json.html).

For an example of how this is done, please look at how
the [TransferProcessMapping](../../extensions/control-plane/store/sql/transfer-process-store-sql/src/main/java/org/eclipse/edc/connector/controlplane/store/sql/transferprocess/store/schema/postgres/TransferProcessMapping.java)
maps a `ResourceManifest`, which in turn contains a `List<ResourceDefinition>` using
the [ResourceManifestMapping](../../extensions/control-plane/store/sql/transfer-process-store-sql/src/main/java/org/eclipse/edc/connector/controlplane/store/sql/transferprocess/store/schema/postgres/ResourceManifestMapping.java)
. Finally, the `SqlQueryStatement`
gets [assembled using the aforementioned JSON array function](../../extensions/common/store/sql/edr-index-sql/src/main/java/org/eclipse/edc/edr/store/index/sql/schema/postgres/PostgresDialectStatements.java)
.

## Prepared statements using the `SqlQueryStatement`

The first step of the translation, i.e. mapping from canonical format onto a target DSL, is handled by
the `TranslationMapping` class and its implementors.

The second and third steps involve interpreting the right-hand operand as scalar or list-type and substituting its
values with the appropriate placeholders for SQL prepared statements. This is done by the `SqlConditionExpression`
class, which validates the expression by checking the operator against a list of supported operators, and then converts
the right-hand operand in to either a single, or a list of placeholders. In SQL this is either `?` or `(?,?,...?)`.

The `SqlQueryStatement` wraps both these two steps (translation and substitution) and offers a convenient API.

## Specific features for PostgreSQL

As mentioned before Postgres has data types and query operators specifically to store and query JSON structures
(see [official documentation](https://www.postgresql.org/docs/12/functions-json.html)). In order to leverage that, some
parts of the object graph are persisted as JSON rather than in a separate table referenced by foreign key.

The condition of the`WHERE` clause is in fact contributed by the `*Mapping` class that is specific to Postgres.

Notes:

- Querying list/array types using Postgres' JSON features requires modifying the `SELECT` statement, as the cast to a
  JSON array is done on the fly. Therefore, this has to be done _before_ constructing the `SqlQueryStatement`.
- in order to port this to another database dialect, the `Mapping` classes very likely will have to be reimplemented

## Supported query operators

Currently, we only support the following operators:

- `=`: equality, right-hand operand is interpreted as literal
- `in`: "one-of", right hand operand must be an `Iterable`
- `like`: pattern matching, right-hand operand is interpreted
  as [pattern string](https://www.w3schools.com/sql/sql_like.asp)

## Comparison with NoSQL databases (CosmosDB)

In CosmosDB this translation is not necessary, as the domain object is wrapped in a `CosmosDocument` and directly
serialized to JSON. In other words, the canonical format is congruent to the CosmosDB schema. However, CosmosDB uses a
similar method to dynamically generate `SELECT` statements.
