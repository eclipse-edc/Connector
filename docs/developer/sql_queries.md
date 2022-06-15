# Issuing dynamic queries in SQL data stores

## Problem statement

The Datamanagement API (DM API) offers a very open and unrestricted query feature: as long as it can be expressed as a
3-tuple in the form

```
<LEFT_OPERAND> <OPERATOR> <RIGHT_OPERAND>
```

it is acceptable. This abstract way of expressing a condition is called a `Criterion`. For example, API clients may
specify a query to obtain a selection of `ContractNegotiation` objects that looks like this:

```
contractAgreement.assetId = myAssetId
```

to specifically return `ContractNegotiation`s, that have a `ContractAgreement` which references an `Asset` with the
ID `"myAssetId"`.

This means, that the backing `SqlContractNegotiationStore` must be able to do the following things:

- dynamically map the expression onto it's internal DB schema
- distinguish between a scalar or a list right-hand operand
- substitute the values in the expression with placeholders for prepared statements (=`?`)
- leverage DB-specific features, like querying a JSON-encoded field (Postgres only)

The following section explains how all of these are achieved.

## The canonical format

Theoretically it is possible to map every schema onto every other schema, given that they are of equal cardinality. To
achieve that, we introduce the notion of a _canonical format_, which is our internal working schema. If in the future we
support a wider variety of translation and transformation paths, everything would have to be transformed into that
canonical format first.

In actuality this matches the field names of our internal Java classes, i.e. our business objects. For instance, a query
for contract negotiations must be specified using the field names of a `ContractNegotiation` object:

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
left-hand operand must look like as if we were traversing the Java object graph. This is what we call the _canonical
format_.

## Using `TranslationMapping`

In order to decouple the canonical format and the SQL schema in terms of naming convention, a mapping scheme exists to
map the canonical model onto the SQL model. The `TranslationMapping` is essentially a graph-like metamodel of the
business objects: every Java class is represented by a `*Mapping` class that contains its field names and the associated
SQL column names.

### Primitive fields:

For fields of primitive datatypes this is trivial: a simple mapping from one onto the other is necessary, for
example, `ContractNegotiation.counterPartyAddress` would be represented in the metamodel as an entry

```java
"counterPartyAddress"->"counterparty_address"
```

### Complex objects:

For fields that are of complex type, such as the `ContractNegotiation.contractAgreement` field, it is necessary to map
the canonical property (= `contractAgreement`) onto yet another `*Mapping` class, in this case it is
the `ContractAgreementMapping`. When resolving a property in the canonical format (`contractAgreement.assetId`), this
means we must recursively descend into the model graph and resolve the correct SQL expression.

## Prepared statements using the `SqlQueryStatement`

The first step of the translation, i.e. mapping from canonical format onto a target DSL, is handled by
the `TranslationMapping` class and its implementors.

The second part involves substituting right-hand values with the appropriate placeholders for SQL prepared statements.
This is done by the `SqlConditionExpression` class, which validates the expression by checking the operator against a
list of supported operators, and then converts the right-hand operand in to either a single, or a list of placeholders.
In SQL this is either `?` or `(?,?,...?)`.

The `SqlQueryStatement` wraps both these two steps (translation and substitution) and offers a convenient API.

## Specific features for PostgreSQL

Postgres has data types and query operators specifically to store and query JSON structures
(see [official documentation](https://www.postgresql.org/docs/12/functions-json.html)). In order to leverage that, some
parts of the object graph are persisted as JSON rather than in a separate table referenced by foreign key.

This means that the translation from canonical format to SQL must take that into account, because the resulting SQL
query will contain those special Postgres JSON operators (`->`, `->>`). For example, `ContractAgreement.policy` (of
type `Policy`) is serialized to JSON and stored thus. A query targeting one of the `Policy` fields could look like this:

```java
contractAgreement.policy.assignee=some-assignee
```

and would get translated into

```sql
SELECT *
FROM edc_contract_negotiation_view
WHERE policy ->> 'assignee' = 'some-assignee'
```

The condition of the`WHERE`
clause is in fact contributed by the `PolicyMapping` class that is specific to Postgres.

Notes:

- a database view is used to flatten the schema and make queries much easier.
- in order to port this to another database dialect, the `PolicyMapping`, and possibly others, would have to be
  reimplemented

## Supported operators

Currently we only support the following operators:

- `=`: equality, right-hand operand is interpreted as literal
- `in`: "one-of", right hand operand must be an `Iterable`
- `like`: pattern matching, right-hand operand is interpreted
  as [pattern string](https://www.w3schools.com/sql/sql_like.asp)

## Comparison with NoSQL databases (CosmosDB)

In CosmosDB this translation is not necessary, as the domain object is wrapped in a `CosmosDocument` and directly
serialized to JSON. In other words, the canonical format is congruent to the CosmosDB schema. However, CosmosDB uses a
similar method to dynamically generate `SELECT` statements.