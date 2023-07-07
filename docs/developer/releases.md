Release Approach
================

## Table of Contents

* [Versioning](#versioning)
    * [API Compatibility](#api-compatibility)
    * [Modules Providing API](#modules-providing-api)
    * [Towards a First Release](#towards-a-first-release)
* [Legal Documentation Requirements](#legal-documentation-requirements)
    * [License and Notice Files](#license-and-notice-files)
    * [Creating the Notice File](#creating-the-notice-file)
    * [Background Information](#background-information)
* [Publishing Maven Artifacts](#publishing-maven-artifacts)
    * [Naming Convention](#naming-convention)

### Versioning

The Eclipse Dataspace Connector will employ [SemVer](https://semver.org) for versioning and distinguish between the
following releases as defined by the [Eclipse Handbook](https://www.eclipse.org/projects/handbook/#release):

- Major releases, which introduce API changes,
- minor releases, which add new functionality, but are API compatible with previous versions, and
- service releases, which include bug fixes only and add no significant new functionality.

Between releases, snapshot versions reflecting the current state of modules can be packaged to distribution artifacts on
a regular basis. Snapshots, however, do not actually represent released versions.

#### API Compatibility

The concept of API compatibility is defined in terms of binary compatibility according to
the [Java SE 17 Language Specification](https://docs.oracle.com/javase/specs/jls/se17/html/jls-13.html)

#### Modules Providing API

The following modules define official extension points of the EDC based on the Java Service Provider Interface (SPI),
contributing public classes, interfaces and public members which are considered public APIs and are, therefore, covered
by the proposed versioning approach:

- spi
- data-protocols/ids-spi

Apart from these SPI-based extension points, individual modules can also contribute additional public-facing APIs, such
as communication endpoints (e.g., based on HTTP). To support a fast-paced development of such endpoints without
impacting the connector's core release cycle, modules contributing this type of public-facing API can be managed within
a separate repository.

The following modules are also distributed as individual artifacts to support a convenient customisation of connectors
and are, however, not considered public APIs:

- core/*
- extensions/*

Extensions can in turn specify their own SPI-based extension points. Theses are, however, regarded as **internal SPI**
and not as a public API. Therefore, changing internal SPI doesn't necessarily imply a version increment for the module.

#### Towards a First Release

Until its first major release, the Eclipse Dataspace Connector will be developed under the version 0.0.1 without
complying to semantic versioning (i.e., API changes don't imply a major release). Snapshot versions may break binary
compatibility with previous versions and should not be regarded as stable. There are no guarantees regarding functional
and non-functional aspects of the implementation. Tooling for a later migration of current implementations to the
envisioned first release will not be provided.

### Legal Documentation Requirements

License and notice files must be included in every unit-level distribution artifact. In the case of Java archive (JAR)
files, the legal files should be placed in the META-INF directory. However, depending on the distribution format, the
exact location of the files might vary.

#### License and Notice Files

An appropriate license file is supplied on the root of the source code repository and must be included as is in each
distribution artifact. The supplied top-level notice file represents a snapshot of the dependencies included in all
modules present in the project repository at a given point in time. Before each new release or distribution, the notice
file must be updated regarding the listed third-party dependencies.  
While distributing individual modules, a notice file containing only the relevant subset of dependencies must be
created (as described below).

#### Creating the DEPENDENCIES File

Notice files consist of some prescribed statements addressing trademarks copyright, and licensing. Additionally, the
section on third-party content lists all dependencies of the current scope (project or module) and must be maintained
before each release. This list is populated by deriving dependencies using the build tool (i.e., gradle), analysing them
using an IP tool (i.e., Eclipse Dash Tool), and decorating the resulting report with additional information using a
custom script. The shell script located below docs/legal supports parsing the results of the Eclipse Dash Licenses tool
and creating a formatted markdown report listing third-party content with extended information.

Execute the gradle task *allDependencies* for creating an integrated dependency report over all sub-modules of the
project (including isolated modules). To process the dependencies of a specific module (e.g., an individual launcher)
execute the standard *dependencies* task:

- First, the dependencies of this module are calculated with gradle and passed to the Dash tool:

```
gradle dependencies | grep -Poh "(?<=\s)[\w.-]+:[\w.-]+:[^:\s]+" | sort | uniq | java -jar /path/org.eclipse.dash.licenses-<VERSION>.jar - -summary DEPENDENCIES
```

> Caution macOS users: by default, macOS has BSD Grep installed, rather than GNU Grep. If you experience any issues,
> please try to [install
GNU Grep](https://apple.stackexchange.com/questions/193288/how-to-install-and-use-gnu-grep-in-macos). Furthermore,
> sorting depends on locale and collation, and
>
may [differ between OSes](https://unix.stackexchange.com/questions/362728/why-does-gnu-sort-sort-differently-on-my-osx-machine-and-linux-machine).
> Our CI job, which verifies the `DEPENDENCIES` file, is running on `ubuntu-latest`

- For each dependency that is reported as `restricted`, an IPlap issue must be opened. For details, please refer to
  the [documentation](https://github.com/eclipse/dash-licenses) of the Dash tool.

#### Background Information

The [Eclipse Dash Licenses tool](https://github.com/eclipse/dash-licenses) first looks
into [IPZilla](https://dev.eclipse.org/ipzilla) and second into [ClearlyDefined](https://clearlydefined.io). IPZilla
tracks the results (i.e. approved/restricted) of IP due diligence conducted by the Eclipse Foundation. The Dash tool
reports for each artifact found within IPZilla also its corresponding contribution questionnaire number (CQ#). In some
cases, an approved artifact doesn't reference a license type, which has to be then searched manually. ClearlyDefined is
maintained by a third-party and assigns scores to artifact licenses. If a minimum threshold is reached, the item is
considered as approved. The Dash tool tags artifacts found within this source accordingly. In some cases, the Dash tool
results in an inappropriate license, although a more suitable one is existing. In this case the tool requests a manual
review. In rare cases neither an Eclipse approval nor an ClearlyDefined entry is found. Currently, these licenses can be
found manually (e.g., on Maven Central).

### Publishing Maven Artifacts

As far as technically sensible, project modules are packaged and distributed as Maven artifacts via third-party
services (i.e., Maven Central).

#### Workflow

Execute the gradle task *publish* on the level of an individual module to publish it as a Maven artifact.

#### Naming Convention

Artifact names must adhere to the following naming convention:

- Group name: org.eclipse.edc
- Artifact id describing the module name (disregarding the directory structure) separating terms by a dash

Examples:

```
org.eclipse.edc:spi
org.eclipse.edc:util
```

A comprehensive list can be found [here](https://search.maven.org/search?q=org.eclipse.edc).

#### Release guide

_Note: the intended audience for this section are individuals who are eligible to author the release process. At the
time of this writing these are the committers of the project._

To trigger a new release please follow these simple steps:

- update `gradle.properties`: set the `version` entry to the new version.
- trigger the actual release in GitHub:
    - on the `Actions` tab pick the `Create EDC Release` workflow
    - Select the `main` branch
    - clicking on `Run workflow` should bring up a prompt for the version string. Please enter the version string in
      SemVer format without any prefixes: `0.0.4-something-SNAPSHOT` would be OK, whereas `v0.0.4-rc1` would not.
    - start the workflow

The GitHub workflow then performs these steps

1. creates a tag on the current branch, e.g. `v0.0.4-something-SNAPSHOT` (note the `v` prefix). This is done using the
   GitHub API.
2. creates a merge commit from source branch to `releases`. The version information is encoded into the commit message.
3. triggers the Eclipse Foundation Jenkins instance ("JIPP"). This is where the actual publishing to MavenCentral
   happens. Note that this process may take quite a bit of time, as every module is signed and uploaded. **Important: if
   the version string contains the `-SNAPSHOT` suffix, the version is uploaded to OSSRH Snapshots instead of
   MavenCentral!**
4. Creates a GitHub release including an automatically generated changelog, if the release is not a `-SNAPSHOT`. This is
   only for informational purposes, no additionsl artifacts are uploaded. The GitHub Release has the following
   properties:
    - only created on non-snapshots
    - always created off of `main` branch
    - the release notes are auto-generated based on the last available tag and the `.github/releases.yaml` file
    - no pre-releases are supported
    - no discussions are created

**Important: The first commit after a release has to change the `defaultVersion` in `gradle.properties` to `-SNAPSHOT`
again. Otherwise, the upload of the automated nightly builds to OSSRH Snapshots will fail.** 
