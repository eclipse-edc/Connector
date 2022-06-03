# Changelog

All notable changes to this project will be documented in this file - formatted and maintained according to the rules
documented on <http://keepachangelog.com>.

This file will not cover changes about documentation, code clean-up, samples, or the CI pipeline. With each version
(respectively milestone), the core features are highlighted. Relevant changes to existing implementations can be found
in the detailed section referring to by linking pull requests or issues.

## [Unreleased]

### Overview

* Bugfixing DataManagementApi
* Build improvements
* Improvements to Dependency Resolution
* Improve and extend documentation

### Detailed Changes

#### Added

* Add domain model documentation (#1158)
* Add gradle test summary (#1148)
* Check to avoid duplicated module names (#1190)
* Implement Catalog service for Data Management API (#1195)
* Add strict body validation for REST endpoints (#1128)
* Add validation on query endpoints (#1258)
* Dependency injection using factory/provider methods (#1056)
* Provisioned resource information in Data Management API (#1221)
* Add custom Jackson (de)serializer for `XMLGregorianCalendar` (#1226)
* Add contract validation rule (#1239)
* Harmonize setting names in `data-plane-transfer` (#1164)
* Add Blob transfer Architectural Decision Record (#1259)
* Add component tests coverage to the codecov coverage report (#1246)
* Postgresql end-to-end test (#1278)
* CosmosDB end-to-end test (#1346)
* Add signing/publishing config (#1147)
* Verify OpenAPI definitions (#1312)
* Documentation for CosmosDB (#1334)

#### Changed

* Restructure sql extension folder tree (#1154)
* Extract single `PolicyArchive` implementation (#1158)
* Replace `accessPolicy` and `contractPolicy` with `accessPolicyId` and `contractPolicyId` on `ContractDefinition` (#1144)
* Replace `policy` with `policyId` on `ContractAgreement` (#1220)
* All DMgmt Api methods now produce and consume `APPLICATION_JSON` (#1175)
* Make data-plane public api controller asynchronous (#1228)
* Provide In-mem implementations by default (#1130)
* Changed Catalog config keys and switched from minutes to seconds
* Uniform all the sql schema files as `docs/schema.sql` (#1278)
* Clean FCC store before updating
* Usage of `NooTransactionContext` in (SQL-)Tests (#1119)
* Sanitize log messages (#1295)
* Improve CosmosDB statement parser (#1282)
* Make `ContractDefinitionService` get definition by its id (#1325)
* Update the CodeQL version from v1 to v2
* Update `slf4j-api` to `2.0.0-alpha7` (#1328)
* Added timestamps to TransferProcess DTO (#1350)
* Make Helm charts more generic (#1363)

#### Removed

* Deprecated Control API (#1310)
* Remove sample module `:extensions:policy:ids-policy` (#1348)
* Unused `:launchers:basic` (#1360)

#### Fixed

* Handle Jakarta exception correctly (#1102)
* Fix Postgres column name (#1108)
* Fix problem with interpreting contractId/negotiationId (#1140)
* Fixed DMgmtApi content types (#1126)
* Fix HTTPS termination in Jetty (#1133)
* Break lease after TransferProcessManager status check (#1214)
* Fix path conflicts between `CatalogApiController` and `FederatedCatalogApiController` (#1225)
* Always use configured IDS API path in IDS webhook address (#1249)
* Fix Azure storage transfer (#1245)
* Throw exception if `IdentityProviderKeyResolver` cannot get keys at startup (#1266)
* Make all the services injectable (#1285)
* Fix CosmosDB Integration tests (#1313)
* Remove ContractDef from Cosmos DB cache when deleting (#1330)
* Fix misleading warning message on initialization (#1336)
* Auto-upload of Cosmos stored procedures (#1338)
* Resiliency against exceptions in the `PartitionManagerImpl` (#1366)

## [milestone-3] - 2022-04-08

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
* Implement S3 data source and data sink for data-plane (#887)
* Add embedded and remote DPF Selector (#832)
* Pass path information into http data source through DPF public API (#929)
* SQL-based TransferProcessStore (#865)
* Add instructions for observability sample with Azure Application Insights (#928)
* Add interface `WebServer` to `web-spi` (#921)
* Add MicrometerExtension integration tests (#935)
* Implement Asset service for Data Management API (#931)
* Implement ContractDefinition service for Data Management API (#940)
* Implement ContractNegotiation service for Data Management API (#957)
* Implement ContractAgreement service for Data Management API (#1048)
* Implement TransferProcess service for Data Management API (#1005)
* SQL-based ContractNegotiationStore (#864)
* In-memory implementation of PolicyStore (#930)
* Add ComponentTest and EndToEndTest annotation (#1034)
* Implement AssetLoader, AssetIndex, DataAddressResolver for SQL (#863)
* Support for HTTP-based provisioning (#963)
* Let Control Plane delegate data transfer to Data Plane (#988)
* Add integration tests for traces (#1035)
* CosmosDb based `PolicyStore` (#826)
* Implement SQL-based PolicyStore (#866)
* Http Provisioner Webhook endpoint (#1039)
* Add Junit tags to categorize tests (#868)
* Add Azure Data Factory extension (#910)
* Add `PolicyService` and Rest endpoints in Data Management API (#1025)
* Add dependency checks (#1000)
* Add `ContractAgreement` query methods on `ContractNegotiationStore` (#1044)
* Add `findById` method to `ContractDefinitionStore` (#967)
* Add `PolicyArchive` for foreign policies (#1072)
* Data plane: control proxy mode with data address toggles (#882)
* Resolve policies using the `PolicyArchive` (#1089)
* Resolve content addresses in the `TransferProcessManager` (#1090)
* Reliably send transfers from consumer to provider (#1007)
* Http Deprovisioner Webhook endpoint (#1039)
* Add performance test example and scheduled workflow (#1029)
* Add basic authentication mechanism for DataManagement API (#981)
* Trace context propagation in DPF (#1162)

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
* Add an overload to `TransactionContext#execute()` (#968)
* Run CosmosDB integration tests on cloud in CI (#964)
* Update the ContractNegotiation service for DataManagementApi (#985)
* Set policy and rule target dynamically when generating contract offers (#609)
* Add SQL-AssetIndex to support `QuerySpec` (#1014)
* Improve provision signalling and align deprovisioning to handle error conditions (#992)
* Replace Asset with assetId on ContractAgreement (#1009)
* Update CI workflow to use concurrency (#1092)
* Adapt system-test to use Embedded DPF to perform file copy (#1060)
* Remove default token-based authentication at the DataManagement API (#981)
*

#### Removed

* Remove ION extension (#664)
* Remove module `:samples:other:commandline` (#820)
* Remove unneeded/unimplemented methods from `TransferProcessStore` (#859)
* Remove module `:samples:other:streaming` (#889)

#### Fixed

* Flaky S3 StatusChecker Test (#794)
* Add missing Data Management Asset controller openapi (#853)
* Policy deserialization (#898)
* Fix extensions loading of EdcRuntimeExtension (#180)
* Fix missing extension to register TPS (#1027)

---

## [milestone-2] - 2021-02-21

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
* Added Policy services and policy Rest endpoints (1025)

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

## [milestone-1] - 2021-12-13

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
