# SQL Participant Context Store

Provides SQL persistence for participant contexts.

Note that the SQL statements (DDL) are specific to and only tested with PostgreSQL. Using it with other RDBMS may work
but might have unexpected side effects!

## Prerequisites

Please apply this [schema](src/main/resources/participant-context-schema.sql) to your SQL database.

## Migrate to optimized indexes

The unique index on `participant_context.participant_context_id` duplicates the primary key and has been removed from
the schema. It can be dropped from an existing database:

```sql
DROP INDEX IF EXISTS participant_context_participant_context_id_uindex;
```
