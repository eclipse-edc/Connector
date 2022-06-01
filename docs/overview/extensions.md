# Extensions



| Module | Description |
| :----- | :---------- |
| :extensions:api                 |  |
| :extensions:aws                 |  |
| :extensions:azure               |  |
| :extensions:catalog             | Includes the `CatalogService` interface, which serves as a generic convenience feature to issue a catalog query to any EDC-compliant connector. |
| :extensions:data-plane          | Covers the Data Plane Framework (DPF), which is the entity in charge of performing the actual data transfer between consumer and provider. |
| :extensions:data-plane-selector |  |
| :extensions:data-plane-transfer | Provides resources used to delegate data transfer to the Data Plane, or to use the Data Plane as a proxy for querying the data. |
| :extensions:dataloading         |  |
| :extensions:filesystem          |  |
| :extensions:http                |  |
| :extensions:http-provisioner    |  |
| :extensions:http-receiver       |  |
| :extensions:iam                 | Provides multiple implementations of identity management. |
| :extensions:jdk-logger-monitor  | Provides a `Logger` which is an implementation of edc `Monitor` interface. |
| :extensions:sql                 |  |
| :extensions:transaction         |  |
| :extensions:transfer-functions  | Provides required services and the SPI for the Transfer Functions feature. |

## :extensions:api

| Module | Description | Details
| :----- | :---------- | :------ |
| :api-core        | contains central elements of any API functionality, such as Exception mappers, etc. | [link]({{extensions}}api/api-core/) |
| :auth-basic      | provides a `AuthenticationService` implementation for basic authentication | [link]({{extensions}}api/auth-basic) |
| :auth-tokenbased |  | [link]({{extensions}}api/auth-tokenbased) |
| :control         |  | [link]({{extensions}}api/control) |
| :data-management | provides a group of modules that build the Data Management API | [link]({{extensions}}api/data-management) |
| :observability   | exposes information about the runtime's health status by accessing the internal `HealthCheckService` | [link]({{extensions}}api/observability) |

## :extensions:aws

| Module | Description | Details
| :----- | :---------- | :------ |
| :aws-test      | runs AWS integration tests | [link]({{extensions}}aws/aws-test) |
| :data-plane-s3 | contains a Data Plane extension to copy data to and from Aws S3 | [link]({{extensions}}aws/data-plane-s3) |
| :s3            |  | [link]({{extensions}}aws/s3) |

## :extensions:azure

| Module | Description | Details
| :----- | :---------- | :------ |
| :azure-test         | runs Azure Blob Integration tests | [link]({{extensions}}azure/azure-test) |
| :blobstorage        |  | [link]({{extensions}}azure/blobstorage) |
| :cosmos             | provides persistent implementations of `AssetIndex`, `ContractDefinition`, etc. using a CosmosDB container | [link]({{extensions}}azure/cosmos) |
| :data-plane:storage | contains a Data Plane extension to copy data to and from Azure Blob storage | [link]({{extensions}}azure/data-plane/storage) |
| :events             |  | [link]({{extensions}}azure/events) |
| :events-config      |  | [link]({{extensions}}azure/events-config) |
| :vault              |  | [link]({{extensions}}azure/vault) |

## :extensions:catalog

| Module | Description | Details
| :----- | :---------- | :------ |
| :federated-catalog-cache | Contains implementations for the Federated Catalog Cache, which is a database that contains a snapshot of all the catalogs offered by all the connectors in a dataspace | [link]({{extensions}}catalog/federated-catalog-cache) |
| :federated-catalog-spi   | This module contains extension points and interfaces specifically for the Federated Catalog feature. | [link]({{extensions}}catalog/federated-catalog-spi) |

## :extensions:data-plane

| Module | Description | Details
| :----- | :---------- | :------ |
| :data-plane-api       |  | [link]({{extensions}}data-plane/data-plane-api) |
| :data-plane-framework |  | [link]({{extensions}}data-plane/data-plane-framework) |
| :data-plane-http      | provides support for sending data sourced from an HTTP endpoint and posting data to an HTTP endpoint | [link]({{extensions}}data-plane/data-plane-http) |
| :data-plane-spi       |  | [link]({{extensions}}data-plane/data-plane-spi) |
| :integration-tests    |  | [link]({{extensions}}data-plane/integration-tests) |

## :extensions:data-plane-selector

| Module | Description | Details
| :----- | :---------- | :------ |
| :selector-api    |  | [link]({{extensions}}data-plane-selector/selector-api) |
| :selector-client | contains implementations for running a DPF Selector embedded in the Control Plane, or as remote instance, accessing it's REST API | [link]({{extensions}}data-plane-selector/selector-client) |
| :selector-core   |  | [link]({{extensions}}data-plane-selector/selector-core) |
| :selector-spi    |  | [link]({{extensions}}data-plane-selector/selector-spi) |
| :selector-store  |  | [link]({{extensions}}data-plane-selector/selector-store) |

## :extensions:data-plane-transfer

| Module | Description | Details
| :----- | :---------- | :------ |
| :data-plane-transfer-client |  | [link]({{extensions}}data-plane-transfer/data-plane-transfer-client) |
| :data-plane-transfer-spi    |  | [link]({{extensions}}data-plane-transfer/data-plane-transfer-spi) |
| :data-plane-transfer-sync   |  | [link]({{extensions}}data-plane-transfer/data-plane-transfer-sync) |

## :extensions:dataloading

No submodules

## :extensions:filesystem

| Module | Description | Details
| :----- | :---------- | :------ |
| :configuration-fs |  | [link]({{extensions}}filesystem/configuration-fs) |
| :vault-fs         |  | [link]({{extensions}}filesystem/vault-fs) |

## :extensions:http

| Module | Description | Details
| :----- | :---------- | :------ |
| :jersey            |  | [link]({{extensions}}http/jersey) |
| :jersey-micrometer |  | [link]({{extensions}}http/jersey-micrometer) |
| :jetty             | provides a `JettyService`, a Servlet Context Container that can expose REST API on a Jersey based WebServer | [link]({{extensions}}http/jetty) |
| :jetty-micrometer  |  | [link]({{extensions}}http/jetty-micrometer) |

## :extensions:http-provisioner

No submodules

## :extensions:http-receiver

No submodules

## :extensions:iam

| Module | Description | Details
| :----- | :---------- | :------ |
| :daps                   |  | [link]({{extensions}}iam/daps) |
| :decentralized-identity | contains modules that implement the "Decentralized Identifier" use case | [link]({{extensions}}iam/decentralized-identity) |
| :iam-mock               |  | [link]({{extensions}}iam/iam-mock) |
| :oauth2                 | provides an `IdentityService` implementation based on the OAuth2 protocol for authorization | [link]({{extensions}}iam/iam-oauth2) |

## :extensions:jdk-logger-monitor

No submodules

## :extensions:sql

| Module | Description | Details
| :----- | :---------- | :------ |
| :asset-index-sql                | provides SQL persistence for assets | [link]({{extensions}}sql/asset-index-sql) |
| :common-sql                     |  | [link]({{extensions}}sql/common-sql) |
| :contract-definition-store-sql  | provides SQL persistence for contract definitions | [link]({{extensions}}sql/contract-definition-store-sql) |
| :contract-negotiation-store-sql | provides SQL persistence for contract negotiations | [link]({{extensions}}sql/contract-negotiation-store-sql) |
| :lease-sql                      |  | [link]({{extensions}}sql/lease-sql) |
| :policy-store-sql               | provides SQL persistence for policies | [link]({{extensions}}sql/policy-store-sql) |
| :pool-sql                       | registers named `javax.sql.DataSource`s to the `org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry` capable of pooling `java.sql.Connection`s | [link]({{extensions}}sql/pool-sql) |
| :transfer-process-store-sql     | provides SQL persistence for transfer processes | [link]({{extensions}}sql/transfer-process-store-sql) |

## :extensions:transaction

| Module | Description | Details
| :----- | :---------- | :------ |
| :transaction-atomikos       |  | [link]({{extensions}}transaction/transaction-atomikos) |
| :transaction-datasource-api |  | [link]({{extensions}}transaction/transaction-datasource-api) |
| :transaction-local          |  | [link]({{extensions}}transaction/transaction-local) |
| :transaction-spi            |  | [link]({{extensions}}transaction/transaction-spi) |

## :extensions:transfer-functions

| Module | Description | Details
| :----- | :---------- | :------ |
| :transfer-functions-core | provides required services for the Transfer Functions feature | [link]({{extensions}}transfer-functions/transfer-functions-core) |
| :transfer-functions-spi  | contains the SPI for the Transfer Functions feature | [link]({{extensions}}transfer-functions/transfer-functions-spi) |
