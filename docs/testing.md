# Writing Tests

## Controlling test verbosity

To run tests verbosely (displaying test events and output and error streams to the console), use the following system property:

```shell
./gradlew test -PverboseTest
```

## Definition and distinction

* _unit tests_ test one single class by stubbing or mocking dependencies.
* [_integration test_](#integration-tests) tests one particular aspect of a software, which may involve external systems.
* [_system tests_](#system-tests) are end-2-end tests that rely on the _entire_ system to be present.

## Integration Tests

### TL;DR

Use integration tests only when necessary, keep them concise, implement them in a defensive manner using timeouts and
randomized names, setup external systems during the workflow.

### When to use them

Generally we should aim at writing unit tests rather than integration tests, because they are simpler, more stable and
typically run faster. Sometimes that's not (easily) possible, especially when an implementation relies on an external
system that is not easily mocked or stubbed such as cloud-based databases.

Therefore, in many cases writing unit tests is more involved that writing an integration test, for example say we wanted
to test our implementation of a CosmosDB-backed queue. We would have to mock the behaviour of the CosmosDB API, which -
while certainly possible - can get complicated pretty quickly. Now we still might do that for simpler scenarios, but
eventually we might want to write an integration test that uses a CosmosDB test instance.

### Coding Guidelines

EDC codebase has few annotations and these annotation focuses on two important aspects:

- Exclude integration tests by default from JUnit test runner as these tests relies on external systems which might not
be available during a local execution.
- Categorize integration tests with help of
[JUnit Tags](https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering).

Following are some available annotations:

- `@IntegrationTest`: Marks an integration test with `IntegrationTest` Junit tag. This is the default tag and can be
used if you do not want to specify any other tags on your test to do further categorization.

Below annotations are used to categorize integration tests based on the runtime components that must be available for
the test to run. All of these annotations are composite annotations and contains `@IntegrationTest` annotation as well.

- `@AzureStorageIntegrationTest`: Marks an integration test with `AzureStorageIntegrationTest` Junit tag. This should be
used when the integration test requires the Azure Storage emulator to run.
- `@AzureCosmosDbIntegrationTest`: Marks an integration test with `AzureCosmosDbIntegrationTest` Junit tag. This should
be used when the integration test requires the Azure CosmosDB emulator to run.
- `@AwsS3IntegrationTest`: Marks an integration test with `AwsS3IntegrationTest` Junit tag. This should be used when the
integration test requires the AWS S3 storage emulator to run.
- `@DapsTest`: Marks an integration test with `DapsIntegrationTest` Junit tag. This should be used when the integration
test is requires Daps IAM endpoint to run.
- `@OpenTelemetryIntegrationTest`: Marks an integration test with `OpenTelemetryIntegrationTest` Junit Tag. This should 
be used for integration tests that require the
[OpenTelemetry agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation), for example tests about
metrics and traces.
- `@EndToEndTest`: Marks an integration test with `EndToEndTest` Junit Tag. This should be used when entire system is 
- involved in a test.
- `@ComponentTest`: Marks an integration test with `ComponentTest` Junit Tag. This should be used when the test does not
use an external system, but uses actual collaborator objects instead of mocks.

We encourage you to use these available annotation but if your integration test does not fit in one of these available
annotations, and you want to categorize them based on their technologies then feel free to create a new annotations but
make sure to use composite annotations which contains `@IntegrationTest`. If you do not wish to categorize based on
their technologies then you can use already available `@IntegrationTest` annotation.

- By default, JUnit test runner ignores all integration tests because in root `build.gradle.kts` file we have excluded
all tests marked with `IntegrationTest` Junit tag.
- If your integration test does not rely on an external system then you may not want to use above-mentioned annotations.

All integration tests should specify annotation to categorize them and the `"...IntegrationTest"` postfix to distinguish
them clearly from unit tests. They should reside in the same package as unit tests because all tests should maintain
package consistency to their test subject.

Any credentials, secrets, passwords, etc. that are required by the integration tests should be passed in using
environment variables. A good way to access them is `ConfigurationFunctions.propOrEnv()` because then the credentials
can also be supplied via system properties.

There is no one-size-fits-all guideline whether to perform setup tasks in the `@BeforeAll` or `@BeforeEach`, it will
depend on the concrete system you're using. As a general rule of thumb long-running one-time setup should be done in
the `@BeforeAll` so as not to extend the run-time of the test unnecessarily. In contrast, in most cases it is **not**
advisable to deploy/provision the external system itself in either of those methods. In other words, provisioning a
CosmosDB or spinning up a Postgres docker container directly from test code should generally be avoided, because it will
introduce code that has nothing to do with the test and may cause security problems (privilege escalation through the
Docker API), etc.

This does not at all discourage the use of external test environments like containers, rather, the external system
should be deployed in the CI script (e.g. through Github's `services` feature), or there might even be a dedicated test
instance running continuously, e.g. a CosmosDB test instance in Azure. In the latter case we need to be careful to avoid
conflicts (e.g. database names) when multiple test runners access that system simultaneously and to properly clean-up
any residue before and after the test.

### Running them locally

As mentioned above the JUnit runner won't pick up integration tests unless a tag is provided. For example to run
`Azure CosmosDB` integration tests pass `includeTags` parameter with tag value to the `gradlew` command:

```bash
./gradlew test -p path/to/module -DincludeTags="AzureCosmosDbIntegrationTest"
```

if needed to run all types of tests(e.g. unit & integration) then it can be achieved by passing the `runAllTests=true`
parameter to the `gradlew` command:

```bash
./gradlew test -DrunAllTests="true"
```

For example to run all integration tests from Azure cosmos db module and its sub-modules:

```bash
./gradlew -p extensions/azure/cosmos test -DincludeTags="AzureCosmosDbIntegrationTest"
```

_Command as `./gradlew :extensions:azure:cosmos test -DincludeTags="AzureCosmosDbIntegrationTest"` does not execute
tests from all sub-modules so we need to use `-p` to specify the module project path._

Cosmos DB integration tests are run by default against a locally running [Cosmos DB Emulator](https://docs.microsoft.com/azure/cosmos-db/local-emulator). You can also use an instance of Cosmos DB running in Azure, in which case you should set the `COSMOS_KEY` and `COSMOS_URL` environment variables.

### Running them in the CI pipeline

All integration tests should go into the [verify workflow](../.github/workflows/verify.yaml), every
"technology" should have its own job, and technology specific tests can be targeted using Junit tags with
`-DincludeTags` property as described above in document.

A GitHub [composite action](https://docs.github.com/actions/creating-actions/creating-a-composite-action) was created to encapsulate the tasks of running tests and uploading test reports as artifacts for publication.

A final job named  `Upload-Test-Report`  should depend on all test jobs. It assembles all individual test reports.

For example let's assume we've implemented a Postgres-based Asset Index, then the integration tests for that should go
into a "Postgres" `job`, and every module that adds a test (here: `extensions:postgres:assetindex`) should apply a
composite annotation (here: `@PostgresIntegrationTest` adding a tag `PostgresIntegrationTest`) on its integration tests.
This tagging will be used by the CI pipeline step to target and execute the integration tests related to Postgres.

Let's also make sure that the code is checked out before and integration tests only run on the upstream repo.

```yaml
jobs:
  Postgres-Integration-Tests:
    # run only on upstream repo
    if: github.repository_owner == 'eclipse-dataspaceconnector'
    runs-on: ubuntu-latest

    # taken from https://docs.github.com/en/actions/using-containerized-services/creating-postgresql-service-containers
    services:
      # Label used to access the service container
      postgres:
        # Docker Hub image
        image: postgres
        # Provide the password for postgres
        env:
          POSTGRES_PASSWORD: ${{ secrets.POSTGRES_PASSWORD }}
        # Set health checks to wait until postgres has started
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    env:
      POSTGRES_USER: ${{ secrets.POSTGRES_USERNAME }}
      POSTGRES_PWD: ${{ secrets.POSTGRES_PASSWORD }}

    steps:
      - uses: ./.github/actions/setup-build

      - name: Postgres Tests   #just an example!
        uses: ./.github/actions/run-tests
        with:
          command: ./gradlew -p extensions/postgres test -DincludeTags="PostgresIntegrationTest"
          
[...]

  Upload-Test-Report:
    needs:
      [...]
      - Postgres-Integration-Tests
    [...]
```

It is important to note that the secrets (here: `POSTGRES_USERNAME` and `POSTGRES_PASSWORD`) must be defined within the
repository's settings and that can only be done by a committer with temporary admin access, so be sure to contact them
before submitting your PR.

### Do's and Don'ts

DO:

- use integration tests sparingly and only when unit tests are not practical
- deploy the external system as `service` directly in the workflow or
- use a dedicated always-on test instance
- take into account that external systems might experience transient failures or have degraded performance, so test
  methods should have a timeout so as not to block the runner indefinitely.
- use randomized strings for things like database/table/bucket/container names, etc., especially when the external
  system does not get destroyed after the test.

DO NOT:

- try to cover everything with integration tests. It's typically a code smell if there are no corresponding unit tests
  for an integration test.
- slip into a habit of testing the external system rather than your usage of it
- store secrets directly in the code. Github will warn about that.
- perform complex external system setup in `@BeforeEach` or `@BeforeAll`

## System tests

System tests are needed when an entire feature should be tested, end to end.

To write a system test two parts are needed:
- _runner_: a module that contains the test logic
- _runtimes_: one or more modules that define a standalone runtime (e.g. a complete EDC definition)

The runner can load an EDC runtime by using the `@RegisterExtension` annotation (example in [`FileTransferIntegrationTest`](../system-tests/tests/src/test/java/org/eclipse/dataspaceconnector/system/tests/local/FileTransferIntegrationTest.java)).

To make sure that the runtime extensions are correctly built and available, they need to be set as dependency of the runner module as `testCompileOnly`. (example in [`build.gradle.kts`](../system-tests/tests/build.gradle.kts)).

This would permit the dependency isolation between runtimes (very important the test need to run two different components like a control plane and a data plane).

## Performance tests

To evaluate performance of the system such tests can be added. Each performance tests should be tagged with
`@PerformanceTest` annotation. To maintain historic data about system performance these tests are executed nightly via
github workflow `performancetests.yml`, test reports are uploaded as an github artifact at end of workflow run.
