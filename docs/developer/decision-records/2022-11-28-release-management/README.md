# Release Management of Dataspace Components

## Decision

EDC's release management will undergo some refactoring/restructuring to be able to accommodate various requirements that
arise from other projects as well as the distributed nature of the components themselves. The term "release management"
solely refers to the [Jenkins build server](https://ci.eclipse.org/edc) we use, i.e. to the "delivery" part in "CI/CD".

## Rationale

The complexity of the EDC project grown quite a bit over the past months so that we now have these separate components,
all of which are hosted in separate Git repositories.

- Build: contains runtime metamodel and Gradle plugins
- Connector (sometimes referred to as the "Core")
- Federated Catalog
- Identity Hub
- Registration Service
- (Minimum Viable Dataspace: does not publish any artifacts)

These components have dependencies onto one another, yet we will use one and the same version for all of them (
cf. [decision record 2022-08-11](../2022-08-11-versioning_and_artifacts)). However, it has become apparent that having a
common release strategy for all of them is necessary to avoid version clashes and maintain our development velocity.

When we publish to MavenCentral, all components must be released with the same version. To avoid feature gaps between
releases, we need to verify compatibility amongst the components on a daily basis.

### Homogenous releases

By that we mean that all components should always have the same version number. That implies, that every component
depends on other components with the _same version number_. For example, version `0.0.3` of the Registration
Service would depend on version `0.0.3` of the Runtime Metamodel, the Connector and the IdentityHub. In turn,
that implies, that before we can build and release Registration Service `0.0.3`, we **must** release that exact
version of all the other components. Only then can we update the dependencies and start the release process.

## Approach

We will separate our releases into two major categories: _automatic_ and _manual_ releases. While the earlier is
triggered either by an external system, such as Github or by a cron job, the latter is done only on-demand upon human
interaction.

### Automatic releases

1. **`SNAPSHOT` builds** are created for every component. Every commit on the `main` branch of every component triggers
   a
   snapshot build.
2. **Nightly components build**: a build job triggers `SNAPSHOT` releases of all components in sequence. The purpose of
   this
   is to verify, that all components are still compatible to each other and to identify broken APIs/SPIs. This works
   because every component uses a `SNAPSHOT` version of the other components, and dependent components are
   built/released first. <br/>
   As it stands, that sequence
   is `runtime-metamodel -> connector -> [federated-catalog, identity-hub] -> registration-service`.
3. **Nightly tagged build**: after the "nightly components build" has successfully completed, every component releases
   a "
   tagged" version, i.e. a snapshot version with metadata that doesn't get overwritten, e.g. `0.0.1.4-SNAPSHOT0221128-SNAPSHOT`.
   In order to make it truly repeatable, every component must update its dependencies to other dependencies.
   The purpose of this is to allow for repeatable builds in client applications, while keeping feature gaps to a
   minimum.

### Manual releases

1. **Release-by-tag**: the build job lets the user select a Git tag and a `VERSION` string as input and builds the
   specified
   version based on the given tag.
2. **Release-by-branch**: the build job lets the user select a Git branch and a `VERSION` string as input and builds the
   specified version based on the given branch.
3. **Release components**: all components are built and released. For actual releases, i.e. artifacts that get published
   to
   MavenCentral, we need a build job that accepts the version as input parameter, and then triggers all downstream
   projects with that same version. This is similar to the "Nightly components build".

## Implementation notes

As many of the aforementioned builds jobs are quite similar, we should try to create reusable pipelines in Jenkins. For
example, a pipeline to build and release all the components is used in two contexts.

One way of doing this is creating a parameterized job, that accepts the Git repo, a version string and an optional git
ref as input. So we have _one_ job, that is invoked with different parameters for every component or release scenario.

Then, once we have that modular pipeline in place, we can create _trigger pipelines_, i.e. pipelines with the sole
purpose of triggering other jobs. They can also have post-build hooks such as notifying Discord or sending emails. 
