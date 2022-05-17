# Code coverage

## Decision

JaCoCo is used for measuring test code coverage in the build, in order to obtain metrics on the current state of EDC testing as well as its evolution over time. 

The Codecov platform is used for visualizing code coverage statistics on PRs. This will raise developer awareness on an increase/decrease of coverage introduced by PRs. Codecov provides a detailed report including a dashboard with additional metrics like code complexity.

## Rationale

Test code coverage is a measure of the source code that executed when a test suite is run. A program with high test coverage has a lower chance of containing bugs.

At the time of writing, code coverage in the solution is under 50%. Increasing code coverage can best be achieved over time by providing feedback on coverage impact on each PR. This requires a more advanced tool than JaCoCo on its own can provide, and is well achieved by Codecov.

## Spikes

We evaluated the following options:

- [JaCoCo without or with aggregation](jacoco.md)
- [JaCoCo with Codecov](codecov.md)
- [JaCoCo with Codacy](codacy.md)
- [JaCoCo with SonarQube](sonarqube.md)
- [JaCoCo with Github Action](jacoco_github_action.md)

## Comparison of selected options

| Tool                       | Project coverage report                | Coverage on PR in Github                                     | Additional comments                                          |
| -------------------------- | -------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| JaCoCo with Codecov        | ✅ Detailed report on Codecov dashboard | ✅ Github bot messages on every PR (coverage after the PR is merged, total project coverage, code complexity) | ✅ Reports on code complexity<p> ✅ Easy configuration       |
| JaCoCo with Github Actions | ✅ Basic report (percentage)            | ✅ Github bot messages on every PR (coverage on changed files and total project coverage) | ⚠️ [Minor issue] Manual configuration (JaCoCo Report Github Action requires a property to path to JaCoCo reports) |
| JaCoCo with Codacy         | ✅ Report available on Codacy dashboard | ⚠️ Not supported                                              | ⚠️ Delays in reports showing up in the dashboard              |
