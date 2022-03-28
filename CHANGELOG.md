
# Changelog

All notable changes to this project will be documented in this file - formatted and maintained according to the rules
documented on <http://keepachangelog.com>.

This file will not cover changes about documentation, code clean-up, samples, or the CI pipeline. With each version (
respectively milestone), the core features are highlighted. Relevant changes to existing implementations can be found in
the detailed section referring to by linking pull requests or issues.

## [Unreleased] - XXXX-XX-XX (Milestone 3)

### Overview

* Removed deprecated code
* Improved CosmosDB interaction
* Added DPF Selector
* Updated Data Management API
* Added SQL as data backend

### Detailed Changes

#### Added

* `ContractDefinitionStore` supports paging (#717)
* Add okhttp client timeouts (#735)
* Unit test framework for Dependency Injection (#843)
* Add a deleteById method to the AssetLoader interface (#880)
* Apply 2-state transition pattern to `ContractNegotiationManager`s (#870)
* Implement S3BucketReader (#675)
* Add configuration setting for state machine batch size (#872)
* Add Jetty context alias for IDS API (#815)
* Add `Hostname` service (#948)
* Add embedded and remote DPF Selector (#832)
* Pass path information into http data source through DPF public API (#929)
* SQL-based TransferProcessStore (#865)
* Add instructions for observability sample with Azure Application Insights (#928)
* Add interface `WebServer` to `web-spi` (#921)
* Implement Asset service for Data Management API (#931)
* Implement ContractDefinition service for Data Management API (#940)

#### Changed

* Change scope used for obtaining a token for ids multipart (#731)
* Refactor ids token validation as extension (#625)
* All `CosmosDocument` subclasses now use a configurable partition key (#780)
* Add `findAll` method to `TransferProcessStore` (#859)
* Add data-management api to the samples (#733)
* Enable pluggable transfer service in DPF (#844)
* Apply 2-state transition pattern to `ContractNegotiationManager`s (#870)
* Apply 2-state transition pattern to `TransferProcessManager` (#831)
* Update build system to Java 17 (#934)
* Refactor (=generify) transformer subsystem (#779)
* Extract interfaces for every api controller class to improve swagger documentation (#891)
* Instrument executors with metrics (#912)
* Call the listeners before the state transition is persisted. (#876)
* Added an overload to `TransactionContext#execute()` (#968)
* Run CosmosDB integration tests on cloud in CI (#964)

#### Removed

* Remove ION extension (#664)
* Remove module `:samples:other:commandline` (#820)
* Remove unneeded/unimplemented methods from `TransferProcessStore` (#859)
* Remove module `:samples:other:streaming` (#889)

#### Fixed

* Flaky S3 StatusChecker Test (#794)
* Added missing Data Management Asset controller openapi (#853)
* Policy deserialization (#898)
* Fix extensions loading of EdcRuntimeExtension (?)

---

## [0.2.0] - 2021-02-21 (Milestone 2)

### Overview

* DataPlaneFramework (DPF): first working version
* Data Management API: controller stubs and OpenApi Spec
* Code Quality: coverage analysis, system test framework
* Traceability and Metrics

### Detailed Changes

#### Added

* Enable Sync and Async `TransferProcesses` (#223)
* Extend `DataRequest` with custom properties field (#300)
* Add consistent update of transfer processes to `TransferProcessManager` (#325)
* Introduce Result abstraction to spi (#365)
* Add basic connector observability (#412)
* Make `Provisioner` async (#451)
* Support ecdsa private keys (#479)
* Prevent override of `RemoteMessageDispatcher` at boot time (#495)
* Add implementation of the Command-Q (#502)
* Add automatic swagger generation (#522)
* Add contract negotiation listener (#524)
* Add support for Portable Transactions (#540)
* Add filtering for properties and making them hierarchical (#555)
* Introduce nbf jwt claim leeway configuration (#595)
* Create Auth SPI for `DataManagementApi` (#598)
* Add `PagingSpec` class (#638)
* Introduce configurable Jetty Contexts and ports (#601)
* Add endpoint for negotiation state to control api (#607)
* Add Data Plane API (#639)
* Add contract-negotiation management endpoints (#640)
* Add API for `ContractDefinition` objects (#641)
* Create DTO entities and controller classes for management api (#647)
* Add command queue for negotiation managers (#652)
* Avoid duplication between runtimes (#654)
* Make `AssetIndex` support paging/querying/filtering (#656)
* Implement Data Plane launcher (#669)
* Create API for `TransferProcesses` (#670)
* Add a `CANCELLED` state to the `TransferProcess` (#671)
* Add Data Plane Server (#680)
* Distribute tracing with OpenTelemetry (#688)
* Create API for policies (#689)
* Add public Api to DPF and make `HttpDataSource` more generic (#699)
* Add an in-memory data plane store (#705)
* Introduce extensions for synchronous data transfer using data plane (#711)

#### Changed

* Replace old aws sdk with the new one (#294)
* Replace `easymock` with `mockito` (#384)
* Isolate `TransferProcessStore` usage into `TransferProcessManager` (#421)
* Migrate the Federated Catalog subsystem over to use IDS Multipart (#443)
* Move over the transfer subsystem to IDS Multipart (#456)
* Update `oauth2-core`: only set `jwsAlgorithm` in constructor and rename var/method in extension (#481)
* Change transfer process state `DEPROVISIONING_REQ` to permit state change to `PROVISIONING` (#498)
* Rename `DistributedIdentity` to `DecentralizedIdentifier` (#500)
* Extract jersey and jetty extensions (#508)
* Rename `DecentralizedIdentifier` to `DecentralizedIdentity` (#517)
* Make audience an intrinsic attribute of the identity service (#521)
* Split core/base into core/base and core/boot (#539)
* Move `WebService` to dedicated spi (#552)
* Rename `protocol-spi` to `web-spi` (#571)
* Make `SELECT_ALL` an empty criteria (#501)
* Register `BlobStoreApi` into `BlobStoreCoreExtension` and merge `blob-core` with `blob-api` (#572)
* Support asset descriptions in ids (#579)
* Map IDS webhook address into `DataRequest` (#676)

#### Removed

* Remove usage of `JsonUnwrapped` (#257)
* Remove old IDS-REST modules and related code (#493)
* Remove deprecated `IdsPolicyService` and related extension (#512)
* Remove `policyId` from `Asset` (#514)
* Get rid of `SchemaRegistry` (#532)
* Remove default `KEYSTORE_PASSWORD` from `FsConfiguration` (#704)

#### Fixed

* Fix modularity drift (#695)

---

## [0.1.0] - 2021-12-13 (Milestone 1)

### Overview

* Setup basic project structure: modules and spi, add mandatory project files
* Asset Index: introduce basic domain model
* Data Transfer: minor improvements
* Distributed Identity: add WEB DID feature
* Federated Cataloging: initial implementation
* IDS: basic ids multipart implementation, self-description, and transformer concept
* Contract Management: implement contract service, contract offer framework, and basic policy negotiation
* Control API: initial implementation of first endpoints

### Detailed Changes

#### Added

* Add basic launcher
* Add contract offer interfaces (#80)
* Add Distributed Identity improvements (#109)
* Add IDS multipart self-description-api (#115)
* Implement the Federated Catalog Cache (#128)
* Add warning and error level to monitoring (#130)
* Add IDS object transformer concept (#148)
* Add Web DID feature
    * Clean-up module dependencies (#153)
    * Implement `DidResolverRegistry` (#163)
    * Implement Web DID Resolver (#179)
* Allow Asset to contain custom properties (#156)
* Implement persistent `AssetIndex` using CosmosDB (#192)
* Create `assetLoader` (#199)
* Add multipart API for `DescriptionRequestMessage`(s) (#208)
* Add ability to expose data contract offers (#217)
* Integrate `ContractService` and policy management
    * Refactor `ContractService` and `ContractOfferFramework` interfaces (#235)
    * Add policy engine implementation and working `ContractDescriptors` (#237)
* Add contract domain obj to spi (#241)
* Add transformer from IDS to EDC (#248)
* Add initial implementation of control API (#259)
* Add cosmos cli loader (#271)
* Send IDS multipart messages (#275)
* Make Oauth2 service compatible with DAPS
* Integrate data request handler functionality in multiple extensions (#276)
* Add ids policy extension (#278)
* Add cosmos `ContractDefinition` store (#282)
* Add contract negotiation capabilities (#284)
    * Introduce types, service interfaces, and integrate policy validation (#269)
    * Add a CosmosDB-based implementation for the `ContractNegotiationStore` (#319)
* Add validation of agreement start/end time (#313)
* Add ids multipart handler for contract messages (#315)
* Add endpoint to the control api to initiate a contract negotiation (#318)
* Add `IN` operator to all `AssetIndex` implementations (#322)
* Support IDS logical constraint transformations (#342)
* Add SQL persistence for contract definitions (#460) (#461)

#### Changed

* Modularize common/utils (#30)
* Set java version to 11 (#91)
* Decouple status checking from provisioned resources (#98)
* Standardize EDC setting key naming: `edc.*` (#113)
* Improve and simplify launcher concept (#114)
* Refactor validation part of OAuth2 identity service (#131)
* Replace `MetadataStore` with `AssetIndex` (#159)
* Remove reflective access from `criterionConverter` (#184)
* Rename `DataCatalog` to `Catalog` in spi, align `CatalogRequest` package name
* Update IDS information model version to v4.2.7 (#249)
* Make dataloader extensible (#263)
* Rename `ContractOfferFramework` to `ContractDefinitionService` (#264)
* Replace usage of `TransferProcessStore` with `TransferProcessManager` (#312)
* Change `ContractOffer` & `ContractAgreement` reference a single asset (#316)

#### Removed

* Remove Atlas and Nifi (#69)
* Remove `CompositeContractOfferFramework` (#154)
* Remove data entry and data catalog entry from spi (#222)

#### Fixed

* Fix stuck processes when they don't have managed resources (#42)
* Fix problem when no `ContractOfferFramework` extension provided (#171)
