# SQL JTI Validation Store

Provides SQL persistence for JTI (JWT ID) validation entries.

Note that the SQL statements (DDL) are specific to and only tested with PostgreSQL. Using it with other RDBMS may work
but might have unexpected side effects!

## Prerequisites

Please apply this [schema](src/main/resources/jti-validation-schema.sql) to your SQL database.

## Migrate to optimized indexes

An index on `expires_at` has been added to `edc_jti_validation`, it supports the periodic deletion of expired entries.
The statements are part of the schema and are idempotent, so they are applied automatically when
`edc.sql.schema.autocreate` is enabled. On large existing tables, consider running them manually with
`CREATE INDEX CONCURRENTLY` to avoid blocking writes while the index is built.

```sql
CREATE INDEX IF NOT EXISTS jti_validation_expires_at_index
    ON edc_jti_validation (expires_at);
```
