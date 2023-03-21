# Splitting the Connector repository

## Decision

The Connector repository will be split up into a "core" repository and technology repositories. Extensions that are
specific to a particular cloud environment will get moved out into new repositories to which we will refer as
"technology repos".
All technology-specific extensions of all the other repos (FederatedCatalog, IdentityHub, RegistrationService) will be
moved out as well.

The Connector repository will _not_ get renamed, but it can henceforth be referred to as "core" or "connector-core".

## Rationale

Many third-party technology extensions are very specific to a particular cloud provider, and cannot reasonably be used
across multiple clouds, thus they depend on specific libraries/SDKs and testing them may even require an account with
that cloud provider.

The reasoning for moving these kinds of extensions out to separate repositories is as follows:

- keeping the core small and agile: building and running it should be fast and efficient, no third-party account should
  be required.
- simplifying the core's CI builds: the build should be self-contained using GitHub services which are docker
  containers. Checking for the presence of account keys or other service credentials in CI builds is thus obsolete.
- reducing build time: not having to build and test many extensions everytime will significantly improve the developer
  experience, because build times will be shorter.
- decoupling responsibilities: contributing in one repository requires only specific knowledge about that
  particular tech, as opposed to: understanding of the _entire_ code base. Maintainer teams with specific knowledge can
  be established.
- different contribution criteria: while the criteria for adoption in the core should be kept very strict, the same
  might
  not necessarily be true for technology repos. There, features could get adopted simply for the sake of having them, as
  long as they are properly maintained. While in the core there would have to be a specific reason to _adopt_ a feature,
  in the technology repos there would have to be a specific reason to _deny_ them.
- decoupling of lifecycles: archiving/abandoning a particular technology repo would not influence the core at all,
  should we ever need to do that.

## Approach

At the time of this writing three technology repositories can be identified:

- `Technology-Azure`
- `Technology-Aws`
- `Technology-Gcp`

Once these repositories are created, all extensions, that contain code for services, that are cloud-specific, will be
moved out accordingly. Initially, this will just be a code dump in the technology repos, but all further development
will then happen there.
To maintain consistency, all specific cloud-provider-extensions will go in the technology repo, e.g. Azure: KeyVault,
BlobStore, CosmosDB etc.

### Continuous Integration

Technology repos should maintain all access keys, service credentials etc. that are needed to run integration tests
against a live testing environment as repo secrets. For example, running i-tests against an actual CosmosDB instance
requires a secret key. These secrets are kept per repository, as opposed to: per GitHub org.

CI workflows must take that into account, and also consider a situation where they are run from a fork.

### Version Catalogs

Technology repositories should publish their own version catalogs. All technology-specific entries will be removed by
the core's version catalog.

## Further Consequences

While the contribution standards for the Connector core will remain high (if anything, they will get raised even more),
technology repositories may have different contribution guidelines. In technology repos, "feature completeness" could
be a sufficient justification to adopt a feature. Cloud providers may want to create a way to run a connector
exclusively using their technology, and for that, they may want a wide variety of services.

Every technology repository should define a team of contributors, who primarily take care of maintaining the code in the
repo.

If a technology repo is not properly maintained, and is not ready for release at a predetermined time, the project
committee may elect to omit it from the release.

When a repository loses its maintainers, or development becomes otherwise stale, the project committee can elect to
archive the repository after an appropriate notice period.