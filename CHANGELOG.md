# Changelog

All notable changes to this project will be documented in this file - formatted and maintained 
according to the rules documented on http://keepachangelog.com.

## [Unreleased]

### Added
* Generate `DataRequest` id on consumer side and forward it into `ArtifactRequestMessage` (#720)
* Add Azure Object Storage Extension (#719)
* `ContractDefinitionStore` supports paging (#717)
* Add Helm chart with minikube tests (#701)
* Add okhttp client timeouts (#735)
* Add management API to sample 3 (#762)
* `nextForState` in CosmosDB disallows re-leasing (#724)
* Add support for cosmosdb emulator (#721)
* Add dpf http system tests (#758)

### Changed
* Code clean up (#713)
* Change scope used for obtaining a token for ids multipart (#731)
* Run JaCoCo on CI only (#746)
* Rename `registerController` to `registerResource`, remove deprecated method (#677)
* Refactor ids token validation as extension (#625)

### Removed

### Fixed
* Fix method naming in `DataManagementApi` (#715)
* Fix `LocalTransactionContext` re-throws exceptions (#727)
* Do not transition from `IN_PROGRESS` to `IN_PROGRESS` during transfer (#729)
* Fix some small bugs (#754)
* Use `OkHttpClient` for tests in `FederatedCatalogCacheEndToEndTest` (#765)

### Other
* Add pull request template (#718)
* Update list of third party licenses (#712)
* Add project and milestone planning to CONTRIBUTING.md (#691)
* Add section about flaky tests to documentation (#769)

## [0.0.2] - 2021-02-21

### Added
* Setup artifacts publication secrets (#350)
* Add consistent update of transfer processes to `TransferProcessManager` (#325)
* Add name method on ServiceExtension (#362)
* Add documentation and sample for `TransferProcessListener` (#375)
* Add warning in case of multiple services of same type (#399)
* Extend `DataRequest` with custom properties field (#300)
* Run azure blobstore integration tests against a local docker container (#389)
* Generate service principal certificate with Terraform in sample 5 (#401)
* Introduce Result abstraction to spi (#365)
* Enable Sync and Async `TransferProcesses` (#223)
* Run s3 integration tests against a local docker container (#377)
* Enable client-secret authentication to AzureVault (#442)
* Add basic connector observability (#412)
* Add guard to avoid NPE when `tokenResult` is failed (#478)
* Re-add the `@Requires` annotation (#480)
* Support ecdsa private keys (#479)
* Make `Provisioner` async (#451)
* Prevent override of `RemoteMessageDispatcher` at boot time (#495)
* Generalize parser in core (#504)
* Evaluate `obtainClientCredentials` result in `data-protocols` (#503)
* Evaluate `ids.webhook.address` at startup and not lazily in `data-protocols` (#492)
* Avoid deprecated `gradle.kts` features to specify main class (#511)
* Add implementation of the Command-Q (#502)
* Handle data flow controller exception (#531)
* Add automatic swagger generation (#522)
* Add contract negotiation listener (#524)
* Add support for Portable Transactions (#540)
* Add filtering for properties and making them hierarchical (#555)
* Add default method for retrieving an int setting (#544)
* Avoid negotiation loop (#545)
* Enhance hierarchical config parsing (#561)
* Support asset descriptions in ids (#579)
* Add endpoint for negotiation state to control api (#607)
* Add `@provides` to `LocalTransactionExtension` (#611)
* Health check service cache can be refreshed at boot completion (#606)
* Add null checks and adapt unit tests to `DataAddress` (#529)
* Apply composition on the `Observable` component over the existing abstraction (#624)
* Introduce nbf jwt claim leeway configuration (#595)
* Add `PagingSpec` class (#638)
* Introduce configurable Jetty Contexts and ports (#601)
* Add generic exception mapper (#630)
* Add API for `ContractDefinition` objects (#641)
* Add Data Plane API (#639)
* Avoid duplication between runtimes (#654)
* Implement Data Plane launcher; minor cosmetic fixes to launch output (#669)
* Don't consider unprocessed entities (#619)
* Add Data Plane Server (#680)
* Add command queue for negotiation managers (#652)
* Add a `CANCELLED` state to the `TransferProcess` (#671)
* Creates Auth SPI for `DataManagementApi` (#598)
* Make `AssetIndex` support paging/querying/filtering (#656)
* Propagate exception with Atomikos (#697)
* Create API for `TransferProcesses` (#670)
* Create API for policies (#689)
* Add support for `QuerySpec` to `ContractNegotiationStore` (#693)
* Introduce code coverage reports with JaCoCo and Codecov (#702)
* Distribute tracing with OpenTelemetry (#688)
* Provide library to support sql data retention sites (#499)
* Map IDS webhook address into `DataRequest` (#676)
* Add custom spans for open telemetry tracing (#694)
* Add contract-negotiation management endpoints (#640)
* Add public Api to DPF and make `HttpDataSource` more generic (#699)
* Create DTO entities and controller classes for management api (#647)
* Fetch entities async (#642)
* Prevent publish workflow execution on forks (#706)
* Add an in-memory data plane store (#705)
* Environment variables are converted to dot-notation Java Properties (#709)
* Initialize store at manager build (#710)
* Introduce extensions for synchronous data transfer using data plane (#711)

### Changed
* Use `CountDownLatch` instead of `Thread.sleep` in test (#351)
* Replace client with consumer (#366)
* Avoid external calls on `DapsIntegrationTest`
* Avoid `MockVault` class duplications
* Use a certificate with longer validity
* Update sample 4 to use multipart (#388)
* Replace old aws sdk with the new one (#294)
* Return json as specified in produces and update readme examples of samples (#411)
* Move duplicated classes to module `core:base` (#396)
* Replace `easymock` with `mockito` (#384)
* Migrate the Federated Catalog subsystem over to use IDS Multipart (#443)
* Isolate `TransferProcessStore` usage into `TransferProcessManager` (#421)
* Increased max test runtime (#449)
* Refactor exception-message and add test for `securityprofile` (#414)
* Rename module (#450)
* Move over the transfer subsystem to IDS Multipart (#456)
* Bump blobstorage and cosmosdb version (#464)
* Refactor module loading (#475)
* Update `oauth2-core`: only set `jwsAlgorithm` in constructor and rename var/method in extension (#481)
* Restructure modules and split SPIs (#482)
* Change transfer process state `DEPROVISIONING_REQ` to permit state change to `PROVISIONING` (#498)
* Rename `DistributedIdentity` to `DecentralizedIdentifier` (#500)
* Extract jersey and jetty extensions (#508)
* Rename `DecentralizedIdentifier` to `DecentralizedIdentity` (#517)
* Improve error message simplifying configuration object (#513)
* Make audience an intrinsic attribute of the identity service (#521)
* Evaluate futures correctly (#530)
* Split core/base into core/base and core/boot (#539)
* Rename single implementers to convention (#558)
* Relocate `ResponseFailure` and `ResponseStatus` to core-spi + move `WebService` to dedicated spi (#552)
* Renamed all packages to `command` (#566)
* Rename `protocol-spi` to `web-spi` (#571)
* Make `SELECT_ALL` an empty criteria (#501)
* Register `BlobStoreApi` into `BlobStoreCoreExtension` and merge `blob-core` with `blob-api` (#572)
* Merge transfer process managers (#554)
* Injection points are only reported as errors when required (#597)
* Minor SPI package clean-up (#621)
* Move interface of asset loader (#516)
* Cleanup Transfer Process SPI (#682)
* Tie together and configure all APIs (#714)

### Removed
* Remove version and groupId from jdk-logger-monitor (#353)
* Remove usage of `JsonUnwrapped` (#257)
* Remove unused `mulesoft` repository (#466)
* Remove old samples to avoid image licensing issues (#471)
* Avoid NPE (#487)
* Remove stray publication from `fcc-node-directory-cosmos` (#488)
* Remove all IDS-REST modules and related code (#493)
* Remove S3-to-S3 sample (#523)
* Remove deprecated `IdsPolicyService` and related extension (#512)
* Remove `policyId` from `Asset` (#514)
* Get rid of `SchemaRegistry` (#532)
* Get rid of dependencies on extensions in core (#684)
* Remove default `KEYSTORE_PASSWORD` from `FsConfiguration` (#704)

### Fixed
* Fix duplicate `artifactId` (#354)
* Fix in transformers related to `Asset` and fix `NullPointerException` in `MultipartCatalogDescriptionRequestSender` (#473)
* Fix `InMemoryFederatedCacheStore` (#474)
* Fix dependency resolution bugs, add blob writer (#486)
* Fix sample 5 (#415)
* Fix incorrect publication names in SPI package (#489)
* Fix duplicated artifact name (#491)
* Fix aws s3 provision in sample 5 (#576)
* Resolve minor issues in contract negotiation process (#666)
* Fix modularity drift (#695)

### Other
* Add documentation about on-boarding new connectors (DistributedIdentity) (#368)
* Create developer instruction for versioning and publishing (#328)
* Fix typo and add client id in oauth2-core readme (#410)
* Update data loader documentation (#457)
* Update transfer states diagram (#454)
* Add policy documentation (#406)
* Update sample path on README (#543)
* Update on-boarding guide (#550)
* Add command-q documentation (#565)

## [0.0.1] - 2021-12-13

### Added
* Add basic launcher
* Run unit test on every commit
* Create on-boarding experience (#61)
* Create and apply checkstyle xml config (#77)
* Add contract offer interfaces (#80)
* Add missing jackson annotation to spi (#94)
* Add s3-to-s3 transfer demo sample (#97)
* Add Distributed Identity improvements (#109)
* Add IDS object transformer concept (#148)
* Add warning and error level to monitoring (#130)
* Allow Asset to contain custom properties (#156)
* Add Web DID feature
  * Clean-up module dependencies (#153)
  * Implement `DidResolverRegistry` (#163)
  * Implement Web DID Resolver (#179)
* Make build fails when checkstyle issues are found (#177)
* Add IDS multipart self-description-api (#115)
* Add sanity check to sample (#201)
* Create `assetLoader` (#199)
* Add asset index demo to sample (#198)
* Implement the Federated Catalog Cache (#128)
* Add multipart API for `DescriptionRequestMessage`(s) (#208)
* Add ability to expose data contract offers (#217)
* Implement persistent AssetIndex using CosmosDB (#192)
* Integrate `ContractService` and policy management
  * Refactor `ContractService` and `ContractOfferFramework` interfaces (#235)
  * Add policy engine implementation and working `ContractDescriptors` (#237)
* Add CosmosDB-based `AssetIndex` integration test to workflow (#243)
* Make Oauth2 service compatible with DAPS
* Add contract domain obj to spi (#241)
* Add contract negotiation capabilities
  * Introduce types, service interfaces, and integrate policy validation (#269)
  * Prepare to integrate data loader (#274)
  * Add a CosmosDB-based implementation for the `ContractNegotiationStore` (#319)
  * Implement contract negotiation (#284)
  * Add ids multipart handler for contract messages (#315)
* Enable/delete `@Disabled` tests (#270)
* Add cosmos cli loader (#271)
* Add transformer from IDS to EDC (#248)
* Add initial implementation of control API (#259)
* Send IDS multipart messages (#275)
* Add ids policy extension (#278)
* Add cosmos `ContractDefinition` store (#282)
* Add controller to fetch resource catalog to control API
* Integrate data request handler functionality in multiple extensions (#276)
* Integrate IDS multipart to samples (#286)
* Add api-key HTTP containerRequestFilter (#320)
* Add jdk-logger-extension using `java.util.logging` (#298)
* Add `IN` operator to all `AssetIndex` implementations (#322)
* Add validation of agreement start/end time (#313)
* Lazily instantiate and warm up the cache (#335)
* Add endpoint to the control api to initiate a contract negotiation (#318)
* Support IDS logical constraint transformations (#342)

### Changed
* Restructure code
* Update package names to `org.eclipse.dataspaceconnector`
* Rename maven publications and artifacts
* Modularize common/utils (#30)
* Moved `StatusCheckerRegistryImpl` to `transfer/` (#36)
* Rename client to consumer (#40)
* Distribute Hackathon code to correct packages (#58)
* Set java version to 11 (#91)
* Decouple status checking from provisioned resources (#98)
* Standardize EDC setting key naming: `edc.*` (#113)
* Improve and simplify launcher concept (#114)
* Rename in-mem artifacts to in-memory (#150)
* Update `ServiceExtension` (#157)
* Refactor validation part of OAuth2 identity service (#131)
* Replace `MetadataStore` with `AssetIndex` (#159)
* Run integration test only on upstream repo, during PR merge
* Prefer `TypeManager` over `ObjectMapper`
* Remove reflective access from `criterionConverter` (#184)
* Update IDS information model version to v4.2.7 (#249)
* Move schema to spi (#252)
* Rename `ContractOfferFramework` to `ContractDefinitionService` (#264)
* Make dataloader extensible (#263)
* Rename and move in memory cache query adapter registry (#265)
* Improve CosmosDB `TransferProcessStore` integration test (#290)
* Correct the lazy-instantiation of the `ContractDefinitionStore` (#293)
* Rename `DataCatalog` to `Catalog` in spi, align `CatalogRequest` package name
* Replace usage of `TransferProcessStore` with `TransferProcessManager` (#312)
* Change `ContractOffer` & `ContractAgreement` reference a single asset (#316)
* Update execution procedures in samples (#332)
* Rename maven artifacts (#314)

### Removed
* Remove docker build
* Remove demo left-over
* Remove Atlas and Nifi (#69)
* Remove `CompositeContractOfferFramework` (#154)
* Remove data entry and data catalog entry from spi (#222)

### Fixed
* Fix license formatting
* Fix stuck processes when they don't have managed resources (#42)
* Fix demo-e2e jar path
* Fix the `AssetIndex` (#142)
* Fix problem when no `ContractOfferFramework` extension provided (#171)
* Fix inconsistent naming (#174)
* Fix broken launcher config and added a sanity check (#226)
* Fix `PermissionToPermissionTransformer` (#224)
* Fix failing test (#273)
* Fix artifact names for data loading modules (#287)
* Fix flaky integration tests (#272)

### Other
* Update `README.md`
* Update intro and launchers section
* Update gaia-x homepage
* Update copyright header
* Update license file
* Add `CONTRIBUTING.md` (#26)
* Add initial Documentation (#28)
* Add diagram for the mvp (#43)
* Remove Microsoft related notes (#57)
* Add simple `SECURITY.md` as requested by EF
* Create legal notice file and script for generating formatted report of 3rd party dependencies (#117)
* Create UML sequence diagram depicting data flow based on HTTP push (#155)
* Add architecture and coding documentation (#175)
* Add docs for integrating and describing data flow sequence diagrams (#181)
* Refine UML sequence diagram for the http push data flow
* Add draft of integration test guidelines
* Update documentation of samples (#321)
* Update incorrect licensing information (#348)
