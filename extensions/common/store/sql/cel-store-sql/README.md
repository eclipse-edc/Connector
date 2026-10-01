# SQL CEL Expression Store

Provides SQL persistence for CEL expressions.

Note that the SQL statements (DDL) are specific to and only tested with PostgreSQL. Using it with other RDBMS may work
but might have unexpected side effects!

## Prerequisites

Please apply this [schema](src/main/resources/cel-expression-schema.sql) to your SQL database.

## Migrate to optimized indexes

An index on `left_operand` has been added to `edc_cel_expression`, it supports the lookup of expressions during policy
evaluation.
The statements are part of the schema and are idempotent, so they are applied automatically when
`edc.sql.schema.autocreate` is enabled. On large existing tables, consider running them manually with
`CREATE INDEX CONCURRENTLY` to avoid blocking writes while the index is built.

```sql
CREATE INDEX IF NOT EXISTS cel_expression_left_operand_index
    ON edc_cel_expression (left_operand);
```
