# Refactoring of testing

The testing pipeline (in GitHub Actions) will be refactored as outlined in this decision record.

## Rationale

Currently the testing pipeline used in EDC is as follows:

- `Unit-Tests`: runs all untagged tests
- `Helm-Chart`: runs the minikube based Helm chart test. Is actually an End-2-End Test
- `Azure-Storage-Integration-Test`: runs all tests in `extensions` and - `system-tests-azure-tests` that are tagged
  with `@AzureStorageIntegrationTest` using the Azurite service. Uploads the Gatling report
- `Azure-Cosmos-Integration-Test`: runs all tests in `extensions` tagged with `@AzureCosmosIntegrationTest`, if
  the `COSMOS_KEY` secret is found. Does **not** run the `EndToEndTransferCosmosDbTest`!!
- `Aws-Integration-Test`: runs all tests in `extensions` tagged with `@AwsS3IntegrationTest` using Minio
- `Daps-Integration-Tests`: runs alls tests in `extensions/common/iam/oauth2/oauth2-daps` tagged
  with `@DapsIntegrationTest`
- `Postgresql-Integration-Tests`: runs all tests tagged with `@PostgresqlIntegrationTest` using the `postgres` service
- `Hashicorp-Vault-Integration-Test`: runs all tests in `extensions` tagged with `@HashicorpVaultIntegrationTest` using
  the `vault` service
- `End-To-End-Test`: runs all tests tagged with `@EndToEndTest` using Minio
- `Component-Test`: runs all tests tagged with `@ComponentTest`
- `Api-Test`: runs all tests tagged with `@ApiTest`

With this structure, there are some problems, which are:

- Tests are run in wrong GH Actions jobs, e.g. `End-To-End` test only executes the inmem variant, the Postgres e2e
  variant is run in the `Postgres-Integration-Test` job
- Tests are incorrectly categorized:
    - The concrete `EndToEnd___Test` instances are tagged with `@PostgresqlIntegrationTest`
      or `@AzureCosmosIntegrationTEst`, but not with `@EndToEndTest`
    - End-to-end tests tagged with `@PostgresqlIntegrationTest` are executed during the `Postgresql-Integration-Test`
      job, and not during the End2End job.
    - The `EndToEndTransferCosmosDbTest` is never executed due to incorrect/missing tags
- Conditional execution based on the existence of secrets (i.e. CosmosDB) is cumbersome (fixed in my latest PR)

## Approach / Solution proposals

### Avoid running untagged tests

Whenever a tag inclusion is specified (`-DincludeTags="..."`), we should _only_ run tests tagged as such. We must avoid
running untagged tests to reduce test runtime, and more clearly delineate test runs.
For that, a small modification to the `TestConvention` in the plugins repo is necessary.

### Create dedicated Gradle tasks

In order to avoid having to specify the exact tag name
as `./gradlew test -DincludeTags="PostgresqlIntegrationTest,AzureCosmosDbIntegrationTest"` we should create dedicated
Gradle tasks to make this more robust toward refactoring. The same command could then be written as:

```bash
./gradlew clean testPostgres testAzureCosmos
```

For example, we could do the following:

```java

@Tag("Api")
public class SomeApiTest {
    @Test
    void foo() {
        fail("api is broken!");
    }
}
```

and correspondingly in the `build.gradle.kts`:

```kotlin
tasks.register<Test>("apiTest") {
    useJUnitPlatform {
        includeTags("Api")
    }
    group = "verification"
}
```

Then, we can execute `./gradlew apiTest`, which will only run the test tagged with `@Tag("Api")`.

A small Gradle Plugin creating these tasks could be implemented in the `buildSrc` directory.

**We must adapt our build plugin's `TestConvention`, because currently it would exclude all `@IntegrationTest` tests
unless the
`includeTags` system property is specified!**

### Refactoring the CI Pipeline

We propose to refactor the current pipeline from its currently very linear form with a single workflow into several
workflows:

- `Verify.yaml`: all other jobs depend on this one
    - `Checkstyle`: job that runs checkstyle
    - `Dependency-Analysis`: runs the dependency analysis
- `Unit-Test.yaml`: runs unit tests with coverage and uploads test reports
- `Api-Tests.yaml`: contains a job that runs all API tests
- `Integration-Test.yaml`: Contains the following jobs, each running the respective integration tests:
    - `Azure-Storage`
    - `Azure-Cosmos`: uses real Cosmos instance, runs only if `COSMOS_KEY` is present
    - `Aws`
    - `Daps`
    - `Hashicorp-Vault`
    - `Component-Test`
- `EndToEndTest.yaml`: runs complex and potentially long running tests. may need more time.
    - `In-Memory`: runs the `EndtoEndTransferInMemoryTest`
    - `CosmosDb`: runs the `EndToEndTransferCosmosDbTest`, only if `COSMOS_KEY` is present
    - `PostgreSQL`: runs the `EndToEndTransferPostgresTest`
    - `Helm-Chart`: runs the minikube-based Helm chart test
    - `OpenTelemetry`: runs the Opentelemetry tests

Moving these jobs out into seperate workflows allows for a higher degree of parallelization, trigger configuration and
error isolation, because only the respective tests are run. Also, triggers are best defined on the workflow level.
Each job uploads their respective test results and coverage data.

_This proposal intentionally ignores Performance tests, because they are as yet unspecified and run on a cron schedule_

### [Optional] Introduction of JUnit Suites

By introducing Suites we can group several tests together, based on class name, package name or by tag. For example,
a `PostgresIntegrationTestSuite` pulls in all postgres integration test, excluding e2e tests, and excluding all untagged
tests.
Similarly, an `EndToEndSuite` would include all `@EndToEndTest` classes.
This is optional, because the same can be achieved with just tags. However, combining multiple Tags (
e.g. `@AzureCosmosDbIntegrationTest && @EndtoEndTest`) becomes easier.
