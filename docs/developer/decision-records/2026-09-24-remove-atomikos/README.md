# Remove the Atomikos transaction extension

## Decision

The `transaction-atomikos` extension and the Atomikos dependencies will be removed from the codebase.
`transaction-local` becomes the only transaction implementation shipped by EDC.

The `TransactionContext` and `DataSourceRegistry` SPIs are not affected, so adopters that need distributed (XA)
transactions can still provide their own implementation as a custom extension.

## Rationale

`transaction-atomikos` provides a JTA/XA-backed `TransactionContext` and `DataSourceRegistry` built on Atomikos
(`com.atomikos:transactions-jta` and `com.atomikos:transactions-jdbc`). In practice it adds cost without a matching
benefit:

- **No need for distributed transactions**: each EDC store runs its transactions against a single datasource. No core
  flow needs XA two-phase commit across several resources, so a JTA transaction manager only adds overhead and
  configuration complexity.
- **Unused and untested end-to-end**: no launcher, system test, or other module in the repository depends on the
  extension. It is covered only by its own unit tests, so regressions in real deployments would go unnoticed.
- **Duplicate configuration model**: the extension defines its own set of about 20 settings (`driver.class`, pool
  sizes, timeouts, `transaction.*`, ...), separate from the `edc.datasource.*` configuration used by
  `transaction-local` together with `sql-pool-apache-commons`. Having two ways to configure datasources makes things
  harder to follow, both for adopters and for maintainers.
- **Maintenance burden**: it is an extra third-party dependency (including the `jakarta` classifier artifact) that has
  to be kept up to date, patched, and checked for licensing.

## Approach

### Components to remove

- The `extensions/common/transaction/transaction-atomikos` module (main sources and tests)
- The `include(":extensions:common:transaction:transaction-atomikos")` entry in `settings.gradle.kts`
- The `atomikos` version and the `atomikos-jta` / `atomikos-jdbc` library entries in `gradle/libs.versions.toml`

No SPI changes are needed. Runtimes that already use `transaction-local` are not affected.

### Migration

Adopters that currently use `transaction-atomikos` should:

- replace it with `transaction-local` and `sql-pool-apache-commons`, and
- move their datasource configuration to the `edc.datasource.<name>.*` settings.

Adopters that really need XA transactions can keep a copy of the extension in their own codebase, since the SPIs it
implements stay the same.
