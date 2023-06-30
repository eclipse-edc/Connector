# Allow and regulate the usage of Testcontainers

## Decision

Using Testcontainers is henceforth allowed, and it is the preferred way to integration-test with third-party software
components.

## Rationale

Testcontainers are a self-contained way to run tests against real third-party software products, such as Postgres. Up
until now the EDC guideline was to separate test code from infrastructure, but this rule is now relaxed in that
integration tests may now use Testcontainers.

However, this does _not_ mean that we should use component tests or integration tests for everything. Or: unit tests are
still the preferred way to test software components, they should continue to make up the largest part of our test code
base.

## Approach

- Every test that uses a Testcontainer must be annotated either with `@ComponentTest` or another suitable integration
  test annotation. So when tests are run locally on developers' machines by default only unit tests are executed.
- New integration/component tests should use Testcontainers
- Existing component/integration tests are gradually converted over to using Testcontainers
