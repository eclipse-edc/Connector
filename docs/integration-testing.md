# Writing Integration Tests

## Definition and distinction

While _unit tests_ test one single class by stubbing or mocking dependencies, and an end-2-end test relies on the _
entire_
system to be present, the _integration test_ tests one particular aspect of a software, which may involve external
systems.

## TL;DR

Use integration tests only when necessary, keep them concise, implement them in a defensive manner using timeouts and
randomized names, setup external systems during the workflow.

## When to use them

Generally we should aim at writing unit tests rather than integration tests, because they are simpler, more stable and
typically run faster. Sometimes that's not (easily) possible, especially when an implementation relies on an external
system that is not easily mocked or stubbed such as cloud-based databases.

Therefore, in many cases writing unit tests is more involved that writing an integration test, for example say we wanted
to test our implementation of a CosmosDB-backed queue. We would have to mock the behaviour of the CosmosDB API, which -
while certainly possible - can get complicated pretty quickly. Now we still might do that for simpler scenarios, but
eventually we might want to write an integration test that uses a CosmosDB test instance.

## Coding Guidelines

An integration test is annotated with `@IntegrationTest`, which causes the test runner to ignore it unless the
`RUN_INTEGRATION_TEST` environment variable is set to `true`.

All integration tests should have the `"...IntegrationTest"` postfix to distinguish them clearly from unit tests. They
should reside in the same package as unit tests because all tests should maintain package consistency to their test
subject.

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

## Running them locally

The JUnit runner won't pick up integration tests unless the `RUN_INTEGRATION_TEST` environment variable is set to `true`
. Also, don't forget to define any credentials that are needed.

Cosmos DB integration tests are run by default against a locally running [Cosmos DB Emulator](https://docs.microsoft.com/azure/cosmos-db/local-emulator). You can also use an instance of Cosmos DB running in Azure, in which case you should set the `COSMOS_KEY` environment variable.

## Running them in the CI pipeline

All integration tests should go into the [integration test workflow](../.github/workflows/integrationtests.yaml),
every "technology" should have its own job, every test should go into a step.

For example let's assume we've implemented a Postgres-based Asset Index, then the integration tests for that should go
into a "Postgres" `job`, and every module that adds a test (here: `extensions:postgres:assetindex`) should go into its
own
`step`. Let's also make sure that the code is checked out before and integration tests only run on the upstream repo.

```yaml
jobs:
  Postgres-Integration-Test:
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

    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v1
        with:
          java-version: '11'

      - name: Postgres Asset Index Test   #just an example!
        env:
          RUN_INTEGRATION_TEST: true
          POSTGRES_USER: ${{ secrets.POSTGRES_USERNAME }}
          POSTGRES_PWD: ${{ secrets.POSTGRES_PASSWORD }}
        run: ./gradlew extensions:postgres:assetindex:check
```

It is important to note that the secrets (here: `POSTGRES_USERNAME` and `POSTGRES_PASSWORD`) must be defined within the
repository's settings and that can only be done by a committer with temporary admin access, so be sure to contact them
before submitting your PR.

## Do's and Don'ts

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