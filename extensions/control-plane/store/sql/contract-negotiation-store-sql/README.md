# SQL-based `ContractNegotiationStore` - technical proposal

**_Note that the SQL statements (DDL) are specific to and only tested with PostgreSQL. Using it with other RDBMS may
work but might have unexpected side effects!_**

## 1. Table schema

see [schema.sql](src/main/resources/contract-negotiation-schema.sql).

As an alternative to storing `ContractAgreement`s in a dedicated table, it could also be serialized and stored as column
in the `contract_negotiation` table. However, we will need to be able to list all contract agreements at some point, so
it seemed more future-proof to have it separate.

## 2. Translating the `ContractNegotiationStore` into SQL statements

All SQL contants such as table names, column names, statement templates etc. are stored in an interface
called `ContractNegotiationStatements`. Column names are declared directly in that interface, as they likely won't
change between different DB technologies, but all statements are then implemented in the `PostgreStatements` class.

## Create a flexible query API to accommodate `QuerySpec`

_For the first version, only the `limit` and `offset` arguments from the `QuerySpec` will be used._

For subsequent versions it is recommended to re-use the `Clause` interface and its implementors, that were originally
implemented for CosmosDB, and create an equivalent set of clauses for SQL. Thus, there would be a `Limit-`, `Offset-`
, `Order-` and `WhereClause` for SQL.

That way, dialect-dependent variants can be implemented should the need arise, because the actual SQL statement is
encoded in those clauses, offering a fluent Java API.

## Migrate to optimized indexes

Indexes supporting participant-scoped queries and the lookup of agreements by asset have been added.
The statements are part of the schema and are idempotent, so they are applied automatically when
`edc.sql.schema.autocreate` is enabled. On large existing tables, consider running them manually with
`CREATE INDEX CONCURRENTLY` to avoid blocking writes while the index is built.

```sql
CREATE INDEX IF NOT EXISTS contract_negotiation_participant_context_id_created_at_index
    ON edc_contract_negotiation (participant_context_id, created_at);

CREATE INDEX IF NOT EXISTS contract_agreement_participant_context_id_index
    ON edc_contract_agreement (agr_participant_context_id, signing_date);

CREATE INDEX IF NOT EXISTS contract_agreement_asset_id_index
    ON edc_contract_agreement (asset_id);
```

The unique indexes on `edc_contract_negotiation.id` and `edc_contract_agreement.agr_id` duplicate the primary keys and
have been removed from the schema. They can be dropped from an existing database:

```sql
DROP INDEX IF EXISTS contract_negotiation_id_uindex;
DROP INDEX IF EXISTS contract_agreement_id_uindex;
```
