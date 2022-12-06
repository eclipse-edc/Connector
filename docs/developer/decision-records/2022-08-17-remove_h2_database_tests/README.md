# Remove H2 as test database

## Decision

All SQL implementations will be changed so that all their tests target an actual Postgres instance instead of an
embedded H2. Test code is currently split up in multiple test classes, one targeting H2, and one for Postgres-specific
code.

## Rationale

While H2 is great for testing generic stuff, it does not support any Postgres-specific features such as JSON queries.
Also, some datatypes behave slightly differently in Postgres. Thus, other than duplicating all test code, the only
option left was for developers to decide which tests should target which database, which is error-prone and almost
impossible to keep consistent. Due to the fact that JUnit `@Tag` annotations are inherited, we cannot simply let the
Postgres tests extend the H2 test, because then excluding them from a test run would not work anymore.

> For that reason the decision was made to only target Postgres, completely ditching H2.

## Approach

- There will be an `abstract` test class, that only contains the `@Test` methods and a reference to the
  system-under-test. Then there will be a Postgres-specific subclass, that initializes the database and instantiates the
  system-under-test. For example, there will be an `AssetIndexTest.java` and a `PostgresAssetIndexTest.java`. That way
  we can easily test multiple SQL implementations against the same test scenario.
- A JUnit extension will be devised that provides the Postgres database connection to deduplicate code. Currently, it is
  created in every test separately.

## Future improvements

Once we actually have another SQL implementation next to Postgres, we could split up the modules into a generic SQL one,
and a postgres specific one, contributing the actual statements and the test code, for example:

```
extensions
├── sql
│   ├── asset-index // <- only contains unit test
│   ├── contract-definition-store
│   ├── ...
├── postgres
│   ├── asset-index-postgres // <- contributes PostgresDialectStatements.java and PostgresAssetIndexTest.java
│   ├── contract-definition-store-postgres
│   ├── ...
├── mssql 
│   ├── asset-index-mssql // <- contributes MssqlDialectStatements.java and MssqlAssetIndexTest.java
│   ├── contract-definition-store-mssql
│   ├── ...

```