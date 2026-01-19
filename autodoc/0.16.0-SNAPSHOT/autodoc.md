Module `accesstokendata-store-sql`
----------------------------------
**Artifact:** org.eclipse.edc:accesstokendata-store-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.store.sql.SqlAccessTokenDataStoreExtension`
**Name:** "Sql AccessTokenData Store"

**Overview:**  Provides Sql Store for {@link AccessTokenData} objects



### Configuration

| Key                                        | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ------------------------------------------ | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.accesstokendata.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.connector.dataplane.store.sql.schema.AccessTokenDataStatements` (optional)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `aes-encryption`
-----------------------
**Artifact:** org.eclipse.edc:aes-encryption:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.encryption.aes.AesEncryptionExtension`
**Name:** "AES Encryption Extension"

**Overview:** No overview provided.


### Configuration

| Key                            | Required | Type     | Default | Pattern | Min | Max | Description                                                     |
| ------------------------------ | -------- | -------- | ------- | ------- | --- | --- | --------------------------------------------------------------- |
| `edc.encryption.aes.key.alias` |          | `string` | ``      |         |     |     | The AES encryption key used for encrypting and decrypting data. |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.encryption.EncryptionAlgorithmRegistry` (required)

Module `api-core`
-----------------
**Artifact:** org.eclipse.edc:api-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.api.ApiCoreDefaultServicesExtension`
**Name:** "ApiCoreDefaultServicesExtension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry`
- `org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationProviderRegistry`

#### Referenced (injected) services
_None_

#### Class: `org.eclipse.edc.api.ApiCoreExtension`
**Name:** "API Core"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

Module `api-observability`
--------------------------
**Artifact:** org.eclipse.edc:api-observability:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.api.observability.ObservabilityApiExtension`
**Name:** "Observability API"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.spi.system.health.HealthCheckService` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)

Module `asset-api`
------------------
**Artifact:** org.eclipse.edc:asset-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.api.management.asset.AssetApiExtension`
**Name:** "Management API: Asset"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (required)

Module `asset-index-sql`
------------------------
**Artifact:** org.eclipse.edc:asset-index-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.AssetStatements`

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.store.sql.assetindex.SqlAssetIndexServiceExtension`
**Name:** "SQL asset index"

**Overview:** No overview provided.


### Configuration

| Key                              | Required | Type     | Default   | Pattern | Min | Max | Description               |
| -------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.asset.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex`
- `org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema.AssetStatements` (optional)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `auth-configuration`
---------------------------
**Artifact:** org.eclipse.edc:auth-configuration:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.api.auth.configuration.ApiAuthenticationConfigurationExtension`
**Name:** "Api Authentication Configuration Extension"

**Overview:** No overview provided.


### Configuration

| Key                            | Required | Type     | Default | Pattern | Min | Max | Description                              |
| ------------------------------ | -------- | -------- | ------- | ------- | --- | --- | ---------------------------------------- |
| `web.http.<context>.auth.type` | `*`      | `string` | ``      |         |     |     | The type of the authentication provider. |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationProviderRegistry` (required)
- `org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `auth-delegated`
-----------------------
**Artifact:** org.eclipse.edc:auth-delegated:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.api.auth.delegated.DelegatedAuthenticationExtension`
**Name:** "Delegating Authentication Service Extension"

**Overview:**  Extension that registers an AuthenticationService that delegates authentication and authorization to a third-party IdP
 and register an {@link ApiAuthenticationProvider} under the type called delegated



### Configuration

| Key                                                             | Required | Type     | Default     | Pattern | Min | Max | Description                                                                                |
| --------------------------------------------------------------- | -------- | -------- | ----------- | ------- | --- | --- | ------------------------------------------------------------------------------------------ |
| `web.http.<context>.auth.dac.key.url`                           | `*`      | `string` | ``          |         |     |     | URL where the third-party IdP's public key(s) can be resolved for the configured <context> |
| `web.http.<context>.auth.dac.cache.validity`                    | `*`      | `Long`   | `300000`    |         |     |     | Duration (in ms) that the internal key cache is valid for the configured <context>         |
| `edc.participant.id`                                            | `*`      | `string` | `anonymous` |         |     |     | Configures the participant id this runtime is operating on behalf of                       |
| `edc.api.auth.dac.validation.tolerance`                         | `*`      | `string` | `5000`      |         |     |     | Default token validation time tolerance (in ms), e.g. for nbf or exp claims                |
| `web.http.<context>.auth.web.http.management.auth.dac.audience` |          | `string` | ``          |         |     |     | Expected audience in the token received by the api management                              |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationProviderRegistry` (required)
- `org.eclipse.edc.token.spi.TokenValidationRulesRegistry` (required)
- `org.eclipse.edc.keys.spi.KeyParserRegistry` (required)
- `org.eclipse.edc.token.spi.TokenValidationService` (required)
- `java.time.Clock` (required)

Module `auth-spi`
-----------------
**Name:** Auth services
**Artifact:** org.eclipse.edc:auth-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.api.auth.spi.AuthenticationService`
  - `org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationProviderRegistry`
  - `org.eclipse.edc.api.auth.spi.ApiAuthenticationProvider`

### Extensions
Module `auth-tokenbased`
------------------------
**Artifact:** org.eclipse.edc:auth-tokenbased:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.api.auth.token.TokenBasedAuthenticationExtension`
**Name:** "Static token API Authentication"

**Overview:**  Extension that registers an AuthenticationService that uses API Keys and register
 an {@link ApiAuthenticationProvider} under the type called tokenbased



### Configuration

| Key                                 | Required | Type     | Default | Pattern | Min | Max | Description                                      |
| ----------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------ |
| `web.http.<context>.auth.key`       | `*`      | `string` | ``      |         |     |     | The api key to use for the <context>             |
| `web.http.<context>.auth.key.alias` | `*`      | `string` | ``      |         |     |     | The vault api key alias to use for the <context> |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationProviderRegistry` (required)

Module `boot`
-------------
**Artifact:** org.eclipse.edc:boot:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.boot.BootServicesExtension`
**Name:** "Boot Services"

**Overview:** No overview provided.


### Configuration

| Key                | Required | Type     | Default         | Pattern | Min | Max | Description                                                                                                                                                |
| ------------------ | -------- | -------- | --------------- | ------- | --- | --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `edc.runtime.id`   | `*`      | `string` | `<random UUID>` |         |     |     | Configures the runtime id. This should be fully or partly randomized, and need not be stable across restarts. It is recommended to leave this value blank. |
| `edc.component.id` | `*`      | `string` | `<random UUID>` |         |     |     | Configures this component's ID. This should be a unique, stable and deterministic identifier.                                                              |

#### Provided services
- `java.time.Clock`
- `org.eclipse.edc.spi.telemetry.Telemetry`
- `org.eclipse.edc.spi.system.health.HealthCheckService`
- `org.eclipse.edc.spi.security.Vault`
- `org.eclipse.edc.spi.system.ExecutorInstrumentation`
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService`

#### Referenced (injected) services
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (optional)

Module `callback-event-dispatcher`
----------------------------------
**Artifact:** org.eclipse.edc:callback-event-dispatcher:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.callback.dispatcher.CallbackEventDispatcherExtension`
**Name:** "Callback dispatcher extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackProtocolResolverRegistry`

#### Referenced (injected) services
- `org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackRegistry` (required)

Module `callback-http-dispatcher`
---------------------------------
**Artifact:** org.eclipse.edc:callback-http-dispatcher:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.callback.dispatcher.http.CallbackEventDispatcherHttpExtension`
**Name:** "Callback dispatcher http extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackProtocolResolverRegistry` (required)
- `org.eclipse.edc.spi.security.Vault` (required)

Module `callback-static-endpoint`
---------------------------------
**Artifact:** org.eclipse.edc:callback-static-endpoint:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.callback.staticendpoint.CallbackStaticEndpointExtension`
**Name:** "Static callbacks extension"

**Overview:**  Extension for configuring the static endpoints for callbacks



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackRegistry` (required)

Module `catalog-api`
--------------------
**Artifact:** org.eclipse.edc:catalog-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.api.management.catalog.CatalogApiExtension`
**Name:** "Management API: Catalog"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (required)

Module `configuration-filesystem`
---------------------------------
**Artifact:** org.eclipse.edc:configuration-filesystem:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.configuration.filesystem.FsConfigurationExtension`
**Name:** "FS Configuration"

**Overview:**  Sources configuration values from a properties file.



### Configuration

| Key             | Required | Type     | Default | Pattern | Min | Max | Description |
| --------------- | -------- | -------- | ------- | ------- | --- | --- | ----------- |
| `edc.fs.config` | `*`      | `string` | ``      |         |     |     |             |

#### Provided services
_None_

#### Referenced (injected) services
_None_

Module `connector-core`
-----------------------
**Artifact:** org.eclipse.edc:connector-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.core.SecurityDefaultServicesExtension`
**Name:** "Security Default Services Extension"

**Overview:**  This extension provides default/standard implementation for the {@link PrivateKeyResolver}
 Those provider methods CANNOT be implemented in {@link CoreDefaultServicesExtension}, because that could potentially cause
 a conflict with injecting/providing the {@link Vault}



### Configuration_None_

#### Provided services
- `org.eclipse.edc.keys.spi.PrivateKeyResolver`
- `org.eclipse.edc.keys.spi.KeyParserRegistry`

#### Referenced (injected) services
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

#### Class: `org.eclipse.edc.connector.core.CoreDefaultServicesExtension`
**Name:** "CoreDefaultServicesExtension"

**Overview:**  This extension provides default/standard implementation for the {@link PrivateKeyResolver}
 Those provider methods CANNOT be implemented in {@link CoreDefaultServicesExtension}, because that could potentially cause
 a conflict with injecting/providing the {@link Vault}



### Configuration_None_

#### Provided services
- `org.eclipse.edc.api.auth.spi.ControlClientAuthenticationProvider`
- `org.eclipse.edc.participant.spi.ParticipantIdMapper`

#### Referenced (injected) services
_None_

#### Class: `org.eclipse.edc.connector.core.LocalPublicKeyDefaultExtension`
**Name:** "Security Default Services Extension"

**Overview:**  This extension provides default/standard implementation for the {@link PrivateKeyResolver}
 Those provider methods CANNOT be implemented in {@link CoreDefaultServicesExtension}, because that could potentially cause
 a conflict with injecting/providing the {@link Vault}



### Configuration

| Key                                  | Required | Type     | Default | Pattern | Min | Max | Description                                                                                                                                      |
| ------------------------------------ | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `edc.iam.publickeys.<pkAlias>.id`    | `*`      | `string` | ``      |         |     |     | ID of the public key.                                                                                                                            |
| `edc.iam.publickeys.<pkAlias>.value` | `*`      | `string` | ``      |         |     |     | Value of the public key. Multiple formats are supported, depending on the KeyParsers registered in the runtime                                   |
| `edc.iam.publickeys.<pkAlias>.path`  | `*`      | `string` | ``      |         |     |     | Path to a file that holds the public key, e.g. a PEM file. Multiple formats are supported, depending on the KeyParsers registered in the runtime |

#### Provided services
- `org.eclipse.edc.keys.spi.LocalPublicKeyService`

#### Referenced (injected) services
- `org.eclipse.edc.keys.spi.KeyParserRegistry` (required)
- `org.eclipse.edc.spi.security.Vault` (required)

#### Class: `org.eclipse.edc.connector.core.CoreServicesExtension`
**Name:** "Core Services"

**Overview:**  This extension provides default/standard implementation for the {@link PrivateKeyResolver}
 Those provider methods CANNOT be implemented in {@link CoreDefaultServicesExtension}, because that could potentially cause
 a conflict with injecting/providing the {@link Vault}



### Configuration_None_

#### Provided services
- `org.eclipse.edc.participant.spi.ParticipantAgentService`
- `org.eclipse.edc.policy.engine.spi.RuleBindingRegistry`
- `org.eclipse.edc.policy.engine.spi.PolicyEngine`
- `org.eclipse.edc.validator.spi.DataAddressValidatorRegistry`
- `org.eclipse.edc.http.spi.ControlApiHttpClient`

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.api.auth.spi.ControlClientAuthenticationProvider` (required)

Module `contract-agreement-api`
-------------------------------
**Artifact:** org.eclipse.edc:contract-agreement-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.api.management.contractagreement.ContractAgreementApiExtension`
**Name:** "Management API: Contract Agreement"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `contract-definition-api`
--------------------------------
**Artifact:** org.eclipse.edc:contract-definition-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.api.management.contractdefinition.ContractDefinitionApiExtension`
**Name:** "Management API: Contract Definition"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (required)

Module `contract-definition-store-sql`
--------------------------------------
**Artifact:** org.eclipse.edc:contract-definition-store-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.store.sql.contractdefinition.SqlContractDefinitionStoreExtension`
**Name:** "SQL contract definition store"

**Overview:** No overview provided.


### Configuration

| Key                                           | Required | Type     | Default   | Pattern | Min | Max | Description               |
| --------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.contractdefinition.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.connector.controlplane.store.sql.contractdefinition.schema.ContractDefinitionStatements` (optional)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `contract-negotiation-api`
---------------------------------
**Artifact:** org.eclipse.edc:contract-negotiation-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.ContractNegotiationApiExtension`
**Name:** "Management API: Contract Negotiation"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (required)

Module `contract-negotiation-store-sql`
---------------------------------------
**Artifact:** org.eclipse.edc:contract-negotiation-store-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.SqlContractNegotiationStoreExtension`
**Name:** "SQL contract negotiation store"

**Overview:** No overview provided.


### Configuration

| Key                                            | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ---------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.contractnegotiation.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.connector.controlplane.store.sql.contractnegotiation.store.schema.ContractNegotiationStatements` (optional)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider` (required)

Module `contract-spi`
---------------------
**Name:** Contract services
**Artifact:** org.eclipse.edc:contract-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore`
  - `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager`
  - `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable`
  - `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard`
  - `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore`
  - `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.NegotiationWaitStrategy`
  - `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ProviderContractNegotiationManager`
  - `org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService`

### Extensions
Module `control-api-configuration`
----------------------------------
**Artifact:** org.eclipse.edc:control-api-configuration:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.api.control.configuration.ControlApiConfigurationExtension`
**Name:** "Control API configuration"

**Overview:**  Tells all the Control API controllers under which context alias they need to register their resources: either
 `default` or `control`



### Configuration

| Key                     | Required | Type     | Default        | Pattern | Min | Max | Description                                                                                                  |
| ----------------------- | -------- | -------- | -------------- | ------- | --- | --- | ------------------------------------------------------------------------------------------------------------ |
| `web.http.control.port` | `*`      | `string` | `9191`         |         |     |     | Port for control api context                                                                                 |
| `web.http.control.path` | `*`      | `string` | `/api/control` |         |     |     | Path for control api context                                                                                 |
| `edc.control.endpoint`  |          | `string` | ``             |         |     |     | Configures endpoint for reaching the Control API. If it's missing it defaults to the hostname configuration. |

#### Provided services
- `org.eclipse.edc.web.spi.configuration.context.ControlApiUrl`

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.spi.system.Hostname` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)

Module `control-plane-aggregate-services`
-----------------------------------------
**Artifact:** org.eclipse.edc:control-plane-aggregate-services:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.services.ControlPlaneServicesExtension`
**Name:** "Control Plane Services"

**Overview:** No overview provided.


### Configuration

| Key                             | Required | Type     | Default | Pattern | Min | Max | Description                                                                         |
| ------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ----------------------------------------------------------------------------------- |
| `edc.policy.validation.enabled` | `*`      | `string` | `true`  |         |     |     | If true enables the policy validation when creating and updating policy definitions |

#### Provided services
- `org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService`
- `org.eclipse.edc.connector.spi.service.SecretService`
- `org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService`
- `org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService`
- `org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService`
- `org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService`
- `org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService`
- `org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService`
- `org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService`
- `org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService`
- `org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService`
- `org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator`
- `org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionProtocolService`

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry` (required)
- `org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager` (required)
- `org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore` (required)
- `org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore` (required)
- `org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.offer.ConsumerOfferResolver` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable` (required)
- `org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `org.eclipse.edc.participant.spi.ParticipantAgentService` (required)
- `org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry` (required)
- `org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver` (required)
- `org.eclipse.edc.spi.command.CommandHandlerRegistry` (required)
- `org.eclipse.edc.validator.spi.DataAddressValidatorRegistry` (required)
- `org.eclipse.edc.spi.iam.IdentityService` (required)
- `org.eclipse.edc.policy.engine.spi.PolicyEngine` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator` (optional)
- `org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry` (required)
- `org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser` (required)
- `org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver` (required)
- `org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController` (required)

Module `control-plane-api`
--------------------------
**Artifact:** org.eclipse.edc:control-plane-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.api.ControlPlaneApiExtension`
**Name:** "Control Plane API"

**Overview:**  {@link ControlPlaneApiExtension } exposes HTTP endpoints for internal interaction with the Control Plane



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `control-plane-api-client`
---------------------------------
**Artifact:** org.eclipse.edc:control-plane-api-client:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.api.client.ControlPlaneApiClientExtension`
**Name:** "Control Plane HTTP API client"

**Overview:**  Extensions that contains clients for Control Plane HTTP APIs



### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient`

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.http.spi.ControlApiHttpClient` (required)

Module `control-plane-catalog`
------------------------------
**Artifact:** org.eclipse.edc:control-plane-catalog:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.catalog.CatalogCoreExtension`
**Name:** "Catalog Core"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.controlplane.catalog.spi.DatasetResolver`

#### Referenced (injected) services
- `org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex` (required)
- `org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore` (required)
- `org.eclipse.edc.connector.controlplane.catalog.spi.DistributionResolver` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore` (required)
- `org.eclipse.edc.policy.engine.spi.PolicyEngine` (required)

#### Class: `org.eclipse.edc.connector.controlplane.catalog.CatalogDefaultServicesExtension`
**Name:** "Catalog Default Services"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry`
- `org.eclipse.edc.connector.controlplane.catalog.spi.DistributionResolver`

#### Referenced (injected) services
- `org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController` (required)

Module `control-plane-contract`
-------------------------------
**Artifact:** org.eclipse.edc:control-plane-contract:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.contract.ContractCoreExtension`
**Name:** "Contract Core"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService`

#### Referenced (injected) services
- `org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex` (required)
- `org.eclipse.edc.policy.engine.spi.PolicyEngine` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.policy.engine.spi.RuleBindingRegistry` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable` (required)

#### Class: `org.eclipse.edc.connector.controlplane.contract.ContractNegotiationDefaultServicesExtension`
**Name:** "Contract Negotiation Default Services"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.controlplane.contract.spi.offer.ConsumerOfferResolver`
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable`
- `org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive`
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard`

#### Referenced (injected) services
- `org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore` (required)
- `org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore` (required)

#### Class: `org.eclipse.edc.connector.controlplane.contract.ContractNegotiationCommandExtension`
**Name:** "Contract Negotiation command handlers"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore` (required)
- `org.eclipse.edc.spi.command.CommandHandlerRegistry` (required)

Module `control-plane-contract-manager`
---------------------------------------
**Artifact:** org.eclipse.edc:control-plane-contract-manager:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.contract.ContractManagerExtension`
**Name:** "Contract Manager"

**Overview:** No overview provided.


### Configuration

| Key                                   | Required | Type     | Default | Pattern | Min | Max | Description                                                                 |
| ------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | --------------------------------------------------------------------------- |
| `state-machine.iteration-wait-millis` | `*`      | `string` | `1000`  |         |     |     | The iteration wait time in milliseconds in the state machine.               |
| `state-machine.batch-size`            | `*`      | `string` | `20`    |         |     |     | The number of entities to be processed on every iteration.                  |
| `send.retry.limit`                    | `*`      | `string` | `7`     |         |     |     | How many times a specific operation must be tried before failing with error |
| `send.retry.base-delay.ms`            | `*`      | `string` | `1000`  |         |     |     | The base delay for the consumer negotiation retry mechanism in millisecond  |
| `state-machine.iteration-wait-millis` | `*`      | `string` | `1000`  |         |     |     | The iteration wait time in milliseconds in the state machine.               |
| `state-machine.batch-size`            | `*`      | `string` | `20`    |         |     |     | The number of entities to be processed on every iteration.                  |
| `send.retry.limit`                    | `*`      | `string` | `7`     |         |     |     | How many times a specific operation must be tried before failing with error |
| `send.retry.base-delay.ms`            | `*`      | `string` | `1000`  |         |     |     | The base delay for the consumer negotiation retry mechanism in millisecond  |

#### Provided services
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager`
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ProviderContractNegotiationManager`

#### Referenced (injected) services
- `org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore` (required)
- `org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver` (required)

Module `control-plane-core`
---------------------------
**Artifact:** org.eclipse.edc:control-plane-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.ControlPlaneDefaultServicesExtension`
**Name:** "Control Plane Default Services"

**Overview:**  Provides default service implementations for fallback



### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex`
- `org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver`
- `org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore`
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore`
- `org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore`
- `org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore`
- `org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackRegistry`
- `org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry`

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

Module `control-plane-transfer`
-------------------------------
**Artifact:** org.eclipse.edc:control-plane-transfer:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.transfer.TransferProcessDefaultServicesExtension`
**Name:** "Transfer Process Default Services"

**Overview:**  Provides core data transfer services to the system.



### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable`
- `org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard`
- `org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser`

#### Referenced (injected) services
_None_

#### Class: `org.eclipse.edc.connector.controlplane.transfer.TransferCoreExtension`
**Name:** "Transfer Core"

**Overview:**  Provides core data transfer services to the system.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)

#### Class: `org.eclipse.edc.connector.controlplane.transfer.TransferProcessCommandExtension`
**Name:** "TransferProcessCommandExtension"

**Overview:**  Provides core data transfer services to the system.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore` (required)
- `org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable` (required)

Module `control-plane-transfer-manager`
---------------------------------------
**Artifact:** org.eclipse.edc:control-plane-transfer-manager:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.transfer.TransferManagerExtension`
**Name:** "Transfer Manager"

**Overview:**  Provides transfer manager service to the system.



### Configuration

| Key                                   | Required | Type     | Default | Pattern | Min | Max | Description                                                                 |
| ------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | --------------------------------------------------------------------------- |
| `state-machine.iteration-wait-millis` | `*`      | `string` | `1000`  |         |     |     | The iteration wait time in milliseconds in the state machine.               |
| `state-machine.batch-size`            | `*`      | `string` | `20`    |         |     |     | The number of entities to be processed on every iteration.                  |
| `send.retry.limit`                    | `*`      | `string` | `7`     |         |     |     | How many times a specific operation must be tried before failing with error |
| `send.retry.base-delay.ms`            | `*`      | `string` | `1000`  |         |     |     | The base delay for the consumer negotiation retry mechanism in millisecond  |

#### Provided services
- `org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager`

#### Referenced (injected) services
- `org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore` (required)
- `org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController` (required)
- `org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable` (required)
- `org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive` (required)
- `org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry` (required)
- `org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry` (required)
- `org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)

Module `core-spi`
-----------------
**Name:** Core services
**Artifact:** org.eclipse.edc:core-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.spi.event.EventRouter`
  - `org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry`
  - `org.eclipse.edc.spi.iam.IdentityService`
  - `org.eclipse.edc.spi.iam.AudienceResolver`
  - `org.eclipse.edc.spi.command.CommandHandlerRegistry`

### Extensions
Module `data-plane`
-------------------
**Artifact:** org.eclipse.edc:data-plane:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.test.e2e.runtime.dataplane.PollingHttpExtension`
**Name:** "PollingHttpExtension"

### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)

Module `data-plane-core`
------------------------
**Artifact:** org.eclipse.edc:data-plane-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.framework.DataPlaneDefaultServicesExtension`
**Name:** "Data Plane Framework Default Services"

**Overview:**  Provides core services for the Data Plane Framework.



### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore`
- `org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore`
- `org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService`
- `org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService`
- `org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService`
- `org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry`
- `org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager`
- `org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager`

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

#### Class: `org.eclipse.edc.connector.dataplane.framework.DataPlaneFrameworkExtension`
**Name:** "Data Plane Framework"

**Overview:**  Provides core services for the Data Plane Framework.



### Configuration

| Key                                             | Required | Type     | Default | Pattern | Min | Max | Description                                                                                                                                              |
| ----------------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `state-machine.iteration-wait-millis`           | `*`      | `string` | `1000`  |         |     |     | The iteration wait time in milliseconds in the state machine.                                                                                            |
| `state-machine.batch-size`                      | `*`      | `string` | `20`    |         |     |     | The number of entities to be processed on every iteration.                                                                                               |
| `send.retry.limit`                              | `*`      | `string` | `7`     |         |     |     | How many times a specific operation must be tried before failing with error                                                                              |
| `send.retry.base-delay.ms`                      | `*`      | `string` | `1000`  |         |     |     | The base delay for the consumer negotiation retry mechanism in millisecond                                                                               |
| `edc.dataplane.state-machine.flow.lease.time`   | `*`      | `string` | `500`   |         |     |     | The time in milliseconds after which a runtime renews its ownership on a started data flow.                                                              |
| `edc.dataplane.state-machine.flow.lease.factor` | `*`      | `string` | `5`     |         |     |     | After flow lease time * factor a started data flow will be considered abandoned by the owner and so another runtime can caught it up and start it again. |
| `edc.dataplane.transfer.threads`                | `*`      | `string` | `20`    |         |     |     | Size of the transfer thread pool. It is advisable to set it bigger than the state machine batch size                                                     |

#### Provided services
- `org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager`
- `org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry`
- `org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer`

#### Referenced (injected) services
- `org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore` (required)
- `org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService` (required)
- `org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry` (required)
- `org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager` (required)
- `org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager` (required)

Module `data-plane-http`
------------------------
**Artifact:** org.eclipse.edc:data-plane-http:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.http.DataPlaneHttpExtension`
**Name:** "Data Plane HTTP"

**Overview:**  Provides support for reading data from an HTTP endpoint and sending data to an HTTP endpoint.



### Configuration

| Key                                      | Required | Type     | Default | Pattern | Min | Max | Description                                                        |
| ---------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------ |
| `edc.dataplane.http.sink.partition.size` | `*`      | `string` | `5`     |         |     |     | Number of partitions for parallel message push in the HttpDataSink |

#### Provided services
- `org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider`

#### Referenced (injected) services
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService` (required)
- `org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `data-plane-http-oauth2-core`
------------------------------------
**Artifact:** org.eclipse.edc:data-plane-http-oauth2-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.http.oauth2.DataPlaneHttpOauth2Extension`
**Name:** "Data Plane HTTP OAuth2"

**Overview:**  Provides support for adding OAuth2 authentication to http data transfer



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider` (required)
- `org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client` (required)

Module `data-plane-iam`
-----------------------
**Artifact:** org.eclipse.edc:data-plane-iam:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.iam.DataPlaneIamDefaultServicesExtension`
**Name:** "Data Plane Default IAM Services"

**Overview:** No overview provided.


### Configuration

| Key                                                 | Required | Type     | Default | Pattern | Min | Max | Description                                                                       |
| --------------------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | --------------------------------------------------------------------------------- |
| `edc.transfer.proxy.token.signer.privatekey.alias`  | `*`      | `string` | ``      |         |     |     | Alias of private key used for signing tokens, retrieved from private key resolver |
| `edc.transfer.proxy.token.verifier.publickey.alias` | `*`      | `string` | ``      |         |     |     | Alias of public key used for verifying the tokens, retrieved from the vault       |

#### Provided services
- `org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessControlService`
- `org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService`

#### Referenced (injected) services
- `org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore` (required)
- `org.eclipse.edc.token.spi.TokenValidationService` (required)
- `org.eclipse.edc.keys.spi.LocalPublicKeyService` (required)
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider` (required)

#### Class: `org.eclipse.edc.connector.dataplane.iam.DataPlaneIamExtension`
**Name:** "Data Plane IAM"

**Overview:** No overview provided.


### Configuration

| Key                  | Required | Type     | Default     | Pattern | Min | Max | Description                                                          |
| -------------------- | -------- | -------- | ----------- | ------- | --- | --- | -------------------------------------------------------------------- |
| `edc.participant.id` | `*`      | `string` | `anonymous` |         |     |     | Configures the participant id this runtime is operating on behalf of |

#### Provided services
- `org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService`

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService` (required)
- `org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessControlService` (required)
- `org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService` (required)
- `org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry` (required)

Module `data-plane-instance-store-sql`
--------------------------------------
**Artifact:** org.eclipse.edc:data-plane-instance-store-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.selector.store.sql.SqlDataPlaneInstanceStoreExtension`
**Name:** "Sql Data Plane Instance Store"

**Overview:**  Extensions that expose an implementation of {@link DataPlaneInstanceStore} that uses SQL as backend storage



### Configuration

| Key                                          | Required | Type     | Default   | Pattern | Min | Max | Description               |
| -------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.dataplaneinstance.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.connector.dataplane.selector.store.sql.schema.DataPlaneInstanceStatements` (optional)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider` (required)

Module `data-plane-kafka`
-------------------------
**Artifact:** org.eclipse.edc:data-plane-kafka:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.kafka.DataPlaneKafkaExtension`
**Name:** "Data Plane Kafka"

**Overview:** No overview provided.


### Configuration

| Key                                       | Required | Type     | Default | Pattern | Min | Max | Description                                   |
| ----------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | --------------------------------------------- |
| `edc.dataplane.kafka.sink.partition.size` | `*`      | `string` | `5`     |         |     |     | The partitionSize used by the kafka data sink |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer` (required)
- `org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService` (required)
- `java.time.Clock` (required)

Module `data-plane-provision-http`
----------------------------------
**Artifact:** org.eclipse.edc:data-plane-provision-http:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.provision.http.DataPlaneProvisionHttpExtension`
**Name:** "Data Plane Provision Http"

**Overview:** No overview provided.


### Configuration

| Key                                     | Required | Type     | Default         | Pattern | Min | Max | Description                                        |
| --------------------------------------- | -------- | -------- | --------------- | ------- | --- | --- | -------------------------------------------------- |
| `web.http.provision.port`               | `*`      | `string` | `8765`          |         |     |     | Port for provision api context                     |
| `web.http.provision.path`               | `*`      | `string` | `/provisioning` |         |     |     | Path for provision api context                     |
| `edc.dataplane.provision.http.endpoint` |          | `string` | ``              |         |     |     | Configures endpoint for reaching the Provision API |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager` (required)
- `org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager` (required)
- `org.eclipse.edc.spi.system.Hostname` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)

Module `data-plane-selector-api`
--------------------------------
**Artifact:** org.eclipse.edc:data-plane-selector-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.selector.DataPlaneSelectorApiExtension`
**Name:** "DataPlane selector API"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)

Module `data-plane-selector-client`
-----------------------------------
**Artifact:** org.eclipse.edc:data-plane-selector-client:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.selector.DataPlaneSelectorClientExtension`
**Name:** "DataPlane Selector client"

**Overview:** No overview provided.


### Configuration

| Key                    | Required | Type     | Default | Pattern | Min | Max | Description                |
| ---------------------- | -------- | -------- | ------- | ------- | --- | --- | -------------------------- |
| `edc.dpf.selector.url` |          | `string` | ``      |         |     |     | DataPlane selector api URL |

#### Provided services
- `org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService`

#### Referenced (injected) services
- `org.eclipse.edc.http.spi.ControlApiHttpClient` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneSelectorManager` (optional)

Module `data-plane-selector-control-api`
----------------------------------------
**Artifact:** org.eclipse.edc:data-plane-selector-control-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.selector.control.api.DataplaneSelectorControlApiExtension`
**Name:** "Dataplane Selector Control API"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (required)

Module `data-plane-selector-core`
---------------------------------
**Artifact:** org.eclipse.edc:data-plane-selector-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.selector.DataPlaneSelectorDefaultServicesExtension`
**Name:** "DataPlaneSelectorDefaultServicesExtension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore`
- `org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry`
- `org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneAvailabilityChecker`

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

#### Class: `org.eclipse.edc.connector.dataplane.selector.DataPlaneSelectorExtension`
**Name:** "Data Plane Selector core"

**Overview:** No overview provided.


### Configuration

| Key                                                  | Required | Type     | Default | Pattern | Min | Max | Description                                                                 |
| ---------------------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | --------------------------------------------------------------------------- |
| `state-machine.iteration-wait-millis`                | `*`      | `string` | `1000`  |         |     |     | The iteration wait time in milliseconds in the state machine.               |
| `state-machine.batch-size`                           | `*`      | `string` | `20`    |         |     |     | The number of entities to be processed on every iteration.                  |
| `send.retry.limit`                                   | `*`      | `string` | `7`     |         |     |     | How many times a specific operation must be tried before failing with error |
| `send.retry.base-delay.ms`                           | `*`      | `string` | `1000`  |         |     |     | The base delay for the consumer negotiation retry mechanism in millisecond  |
| `edc.data.plane.selector.state-machine.check.period` | `*`      | `string` | `60`    |         |     |     | the check period for data plane availability, in seconds                    |

#### Provided services
- `org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneSelectorManager`
- `org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService`

#### Referenced (injected) services
- `org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry` (required)
- `org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneAvailabilityChecker` (required)

Module `data-plane-selector-spi`
--------------------------------
**Name:** DataPlane selector services
**Artifact:** org.eclipse.edc:data-plane-selector-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient`
  - `org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService`

### Extensions
Module `data-plane-self-registration`
-------------------------------------
**Artifact:** org.eclipse.edc:data-plane-self-registration:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.registration.DataplaneSelfRegistrationExtension`
**Name:** "Dataplane Self Registration"

**Overview:** No overview provided.


### Configuration

| Key                                  | Required | Type     | Default | Pattern | Min | Max | Description                                                                              |
| ------------------------------------ | -------- | -------- | ------- | ------- | --- | --- | ---------------------------------------------------------------------------------------- |
| `edc.data.plane.self.unregistration` | `*`      | `string` | `false` |         |     |     | Enable data-plane un-registration at shutdown (not suggested for clustered environments) |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService` (required)
- `org.eclipse.edc.web.spi.configuration.context.ControlApiUrl` (required)
- `org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService` (required)
- `org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry` (required)
- `org.eclipse.edc.spi.system.health.HealthCheckService` (required)
- `org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager` (required)

Module `data-plane-signaling`
-----------------------------
**Artifact:** org.eclipse.edc:data-plane-signaling:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.signaling.DataPlaneSignalingAvailabilityCheckerExtension`
**Name:** "Data Plane Signaling Availability Checker"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneAvailabilityChecker`
- `org.eclipse.edc.signaling.port.ClientFactory`

#### Referenced (injected) services
- `org.eclipse.edc.http.spi.ControlApiHttpClient` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

#### Class: `org.eclipse.edc.signaling.DataPlaneSignalingApiExtension`
**Name:** "Data Plane Signaling Api"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService` (required)

#### Class: `org.eclipse.edc.signaling.DataPlaneSignalingFlowControllerExtension`
**Name:** "Data Plane Signaling Api"

**Overview:** No overview provided.


### Configuration

| Key                                      | Required | Type     | Default  | Pattern | Min | Max | Description                                                                                              |
| ---------------------------------------- | -------- | -------- | -------- | ------- | --- | --- | -------------------------------------------------------------------------------------------------------- |
| `edc.dataplane.client.selector.strategy` | `*`      | `string` | `random` |         |     |     | Defines strategy for Data Plane instance selection in case Data Plane is not embedded in current runtime |

#### Provided services
- `org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController`

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.web.spi.configuration.context.ControlApiUrl` (required)
- `org.eclipse.edc.signaling.port.ClientFactory` (required)
- `org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService` (required)

Module `data-plane-signaling-api`
---------------------------------
**Artifact:** org.eclipse.edc:data-plane-signaling-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.api.DataPlaneSignalingApiExtension`
**Name:** "DataPlane Signaling API extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `data-plane-signaling-client`
------------------------------------
**Artifact:** org.eclipse.edc:data-plane-signaling-client:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.client.DataPlaneSignalingClientTransformExtension`
**Name:** "Legacy Data Plane Signaling transform Client"

**Overview:**  This extension provides an implementation of {@link DataPlaneClient} compliant with the data plane signaling protocol



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

#### Class: `org.eclipse.edc.connector.dataplane.client.LegacyDataPlaneSignalingClientExtension`
**Name:** "Legacy Data Plane Signaling Client"

**Overview:**  This extension provides an implementation of {@link DataPlaneClient} compliant with the data plane signaling protocol



### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory`
- `org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneAvailabilityChecker`

#### Referenced (injected) services
- `org.eclipse.edc.http.spi.ControlApiHttpClient` (optional)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager` (optional)

Module `data-plane-spi`
-----------------------
**Name:** DataPlane services
**Artifact:** org.eclipse.edc:data-plane-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager`
  - `org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry`
  - `org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient`
  - `org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService`
  - `org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessControlService`
  - `org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService`

### Extensions
Module `data-plane-store-sql`
-----------------------------
**Artifact:** org.eclipse.edc:data-plane-store-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.dataplane.store.sql.SqlDataPlaneStoreExtension`
**Name:** "Sql Data Plane Store"

**Overview:**  Provides Sql Store for Data Plane Flow Requests states



### Configuration

| Key                                  | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ------------------------------------ | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.dataplane.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.connector.dataplane.store.sql.schema.DataFlowStatements` (optional)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider` (required)

Module `decentralized-claims-core`
----------------------------------
**Artifact:** org.eclipse.edc:decentralized-claims-core:0.16.0-SNAPSHOT

**Categories:** _iam, transform, jsonld, iam, transform, jsonld_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.decentralizedclaims.core.DcpTransformExtension`
**Name:** "DCP Transform Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

#### Class: `org.eclipse.edc.iam.decentralizedclaims.core.DcpCoreExtension`
**Name:** "DCP Core Extension"

**Overview:** No overview provided.


### Configuration

| Key                                            | Required | Type     | Default  | Pattern | Min | Max | Description                                                                         |
| ---------------------------------------------- | -------- | -------- | -------- | ------- | --- | --- | ----------------------------------------------------------------------------------- |
| `edc.iam.issuer.id`                            | `*`      | `string` | ``       |         |     |     | DID of the participant                                                              |
| `edc.iam.credential.revocation.cache.validity` | `*`      | `string` | `900000` |         |     |     | Validity period of cached StatusList2021 credential entries in milliseconds.        |
| `edc.sql.store.jti.cleanup.period`             | `*`      | `string` | `60`     |         |     |     | The period of the JTI entry reaper thread in seconds                                |
| `edc.iam.accesstoken.jti.validation`           | `*`      | `string` | `true`   |         |     |     | Activate or deactivate JTI validation                                               |
| `edc.iam.credential.revocation.mimetype`       | `*`      | `string` | `*/*`    |         |     |     | A comma-separated list of accepted content types of the revocation list credential. |

#### Provided services
- `org.eclipse.edc.spi.iam.IdentityService`
- `org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier`

#### Referenced (injected) services
- `org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService` (required)
- `org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry` (required)
- `org.eclipse.edc.token.spi.TokenValidationService` (required)
- `org.eclipse.edc.token.spi.TokenValidationRulesRegistry` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver` (required)
- `org.eclipse.edc.iam.decentralizedclaims.spi.ClaimTokenCreatorFunction` (required)
- `org.eclipse.edc.participant.spi.ParticipantAgentService` (required)
- `org.eclipse.edc.iam.decentralizedclaims.spi.DcpParticipantAgentServiceExtension` (optional)
- `org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry` (required)
- `org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig` (required)
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.iam.decentralizedclaims.spi.PresentationRequestService` (required)

#### Class: `org.eclipse.edc.iam.decentralizedclaims.core.DcpPresentationRequestExtension`
**Name:** "DCP Presentation Request Extension"

**Overview:** No overview provided.


### Configuration

| Key                  | Required | Type     | Default | Pattern | Min | Max | Description                                       |
| -------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------- |
| `edc.dcp.v08.forced` |          | `string` | `false` |         |     |     | If set enable the dcp v0.8 namespace will be used |

#### Provided services
- `org.eclipse.edc.iam.decentralizedclaims.spi.PresentationRequestService`
- `org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceClient`

#### Referenced (injected) services
- `org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService` (required)
- `org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)

#### Class: `org.eclipse.edc.iam.decentralizedclaims.core.DcpScopeExtractorExtension`
**Name:** "DCP scope extractor extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.policy.engine.spi.PolicyEngine` (required)
- `org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractorRegistry` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

#### Class: `org.eclipse.edc.iam.decentralizedclaims.core.DcpDefaultServicesExtension`
**Name:** "DCP Extension to register default services"

**Overview:** No overview provided.


### Configuration

| Key                            | Required | Type     | Default | Pattern | Min | Max | Description                                                                                                                                                |
| ------------------------------ | -------- | -------- | ------- | ------- | --- | --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `edc.iam.sts.privatekey.alias` |          | `string` | ``      |         |     |     | Alias of private key used for signing tokens, retrieved from private key resolver. Required when using EmbeddedSTS                                         |
| `edc.iam.sts.publickey.id`     |          | `string` | ``      |         |     |     | Key Identifier used by the counterparty to resolve the public key for token validation, e.g. did:example:123#public-key-1. Required when using EmbeddedSTS |
| `edc.iam.sts.token.expiration` | `*`      | `string` | `5`     |         |     |     | Self-issued ID Token expiration in minutes. By default is 5 minutes                                                                                        |

#### Provided services
- `org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry`
- `org.eclipse.edc.iam.decentralizedclaims.spi.verification.SignatureSuiteRegistry`
- `org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction`
- `org.eclipse.edc.iam.decentralizedclaims.spi.scope.ScopeExtractorRegistry`
- `org.eclipse.edc.spi.iam.AudienceResolver`
- `org.eclipse.edc.iam.decentralizedclaims.spi.ClaimTokenCreatorFunction`

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider` (required)
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore` (required)

Module `decentralized-claims-issuers-configuration`
---------------------------------------------------
**Artifact:** org.eclipse.edc:decentralized-claims-issuers-configuration:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.decentralizedclaims.issuer.configuration.TrustedIssuerConfigurationExtension`
**Name:** "Trusted Issuers Configuration Extensions"

**Overview:**  This DCP extension makes it possible to configure a list of trusted issuers, that will be matched against the Verifiable Credential issuers.



### Configuration

| Key                                                   | Required | Type     | Default | Pattern | Min | Max | Description                                         |
| ----------------------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | --------------------------------------------------- |
| `edc.iam.trusted-issuer.<issuerAlias>.id`             | `*`      | `string` | ``      |         |     |     | ID of the issuer.                                   |
| `edc.iam.trusted-issuer.<issuerAlias>.properties`     | `*`      | `string` | ``      |         |     |     | Additional properties of the issuer.                |
| `edc.iam.trusted-issuer.<issuerAlias>.supportedtypes` | `*`      | `string` | ``      |         |     |     | List of supported credential types for this issuer. |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `decentralized-claims-sts-remote-client`
-----------------------------------------------
**Artifact:** org.eclipse.edc:decentralized-claims-sts-remote-client:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.decentralizedclaims.sts.remote.client.StsRemoteClientExtension`
**Name:** "Sts remote client extension"

**Overview:**  Configuration Extension for the STS OAuth2 client



### Configuration

| Key                                     | Required | Type     | Default | Pattern | Min | Max | Description                                |
| --------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------ |
| `edc.iam.sts.oauth.token.url`           | `*`      | `string` | ``      |         |     |     | STS OAuth2 endpoint for requesting a token |
| `edc.iam.sts.oauth.client.id`           | `*`      | `string` | ``      |         |     |     | STS OAuth2 client id                       |
| `edc.iam.sts.oauth.client.secret.alias` | `*`      | `string` | ``      |         |     |     | Vault alias of STS OAuth2 client secret    |

#### Provided services
- `org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService`

#### Referenced (injected) services
- `org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client` (required)
- `org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig` (required)
- `org.eclipse.edc.spi.security.Vault` (required)

Module `dsp-catalog-http-api-2025`
----------------------------------
**Artifact:** org.eclipse.edc:dsp-catalog-http-api-2025:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.catalog.http.api.v2025.DspCatalogApi2025Extension`
**Name:** "Dataspace Protocol 2025/1 API Catalog Extension"

**Overview:**  Creates and registers the controller for dataspace protocol v2025/1 catalog requests.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogProtocolService` (required)
- `org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)
- `org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (required)

Module `dsp-catalog-http-dispatcher`
------------------------------------
**Artifact:** org.eclipse.edc:dsp-catalog-http-dispatcher:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.catalog.http.dispatcher.DspCatalogHttpDispatcherExtension`
**Name:** "Dataspace Protocol Catalog HTTP Dispatcher Extension"

**Overview:**  Creates and registers the HTTP dispatcher delegate for sending a catalog request as defined in
 the dataspace protocol specification.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher` (required)
- `org.eclipse.edc.protocol.dsp.http.spi.serialization.JsonLdRemoteMessageSerializer` (required)
- `org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspRequestBasePathProvider` (required)

Module `dsp-catalog-transform-2025`
-----------------------------------
**Artifact:** org.eclipse.edc:dsp-catalog-transform-2025:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.catalog.transform.v2025.DspCatalogTransformV2025Extension`
**Name:** "Dataspace Protocol 2025/1 Catalog Transform Extension"

**Overview:**  Provides the transformers for DSP v2025/1 catalog message types via the {@link TypeTransformerRegistry}.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.participant.spi.ParticipantIdMapper` (required)

Module `dsp-http-api-base-configuration`
----------------------------------------
**Artifact:** org.eclipse.edc:dsp-http-api-base-configuration:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.http.api.configuration.DspApiBaseConfigurationExtension`
**Name:** "Dataspace Protocol API Base Configuration Extension"

**Overview:** No overview provided.


### Configuration

| Key                        | Required | Type     | Default         | Pattern | Min | Max | Description                                                                                            |
| -------------------------- | -------- | -------- | --------------- | ------- | --- | --- | ------------------------------------------------------------------------------------------------------ |
| `web.http.protocol.port`   | `*`      | `string` | `8282`          |         |     |     | Port for protocol api context                                                                          |
| `web.http.protocol.path`   | `*`      | `string` | `/api/protocol` |         |     |     | Path for protocol api context                                                                          |
| `edc.dsp.callback.address` |          | `string` | ``              |         |     |     | Configures endpoint for reaching the Protocol API in the form "<hostname:protocol.port/protocol.path>" |

#### Provided services
- `org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress`

#### Referenced (injected) services
- `org.eclipse.edc.spi.system.Hostname` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `dsp-http-api-configuration-2025`
----------------------------------------
**Artifact:** org.eclipse.edc:dsp-http-api-configuration-2025:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.http.api.configuration.v2025.DspApiConfigurationV2025Extension`
**Name:** "Dataspace Protocol 2025/1 API Configuration Extension"

**Overview:**  Registers protocol webhook, API transformers and namespaces for DSP v2025/1.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.participant.spi.ParticipantIdMapper` (required)
- `org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress` (required)
- `org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry` (required)
- `org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction` (required)

Module `dsp-http-core`
----------------------
**Artifact:** org.eclipse.edc:dsp-http-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.http.DspHttpCoreExtension`
**Name:** "Dataspace Protocol Core Extension"

**Overview:**  Provides an implementation of {@link DspHttpRemoteMessageDispatcher} to support sending dataspace
 protocol messages. The dispatcher can then be used by other extensions to add support for
 specific message types.



### Configuration

| Key                               | Required | Type     | Default | Pattern | Min | Max | Description                                                      |
| --------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ---------------------------------------------------------------- |
| `edc.dsp.well-known-path.enabled` |          | `string` | `false` |         |     |     | If set enable the well known path resolution scheme will be used |

#### Provided services
- `org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher`
- `org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler`
- `org.eclipse.edc.protocol.dsp.http.spi.serialization.JsonLdRemoteMessageSerializer`
- `org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry`
- `org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspRequestBasePathProvider`

#### Referenced (injected) services
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.spi.iam.IdentityService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.token.spi.TokenDecorator` (optional)
- `org.eclipse.edc.policy.engine.spi.PolicyEngine` (required)
- `org.eclipse.edc.spi.iam.AudienceResolver` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry` (required)

Module `dsp-http-dispatcher-2025`
---------------------------------
**Artifact:** org.eclipse.edc:dsp-http-dispatcher-2025:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.http.dispatcher.DspHttpDispatcherV2025Extension`
**Name:** "DspHttpDispatcherV2025Extension"

### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry` (required)
- `org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher` (required)

Module `dsp-negotiation-http-api-2025`
--------------------------------------
**Artifact:** org.eclipse.edc:dsp-negotiation-http-api-2025:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.negotiation.http.api.v2025.DspNegotiationApi2025Extension`
**Name:** "Dataspace Protocol Negotiation Api v2025/1"

**Overview:**  Creates and registers the controller for dataspace protocol v2025/1 negotiation requests.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler` (required)
- `org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (required)

Module `dsp-negotiation-http-dispatcher`
----------------------------------------
**Artifact:** org.eclipse.edc:dsp-negotiation-http-dispatcher:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.negotiation.http.dispatcher.DspNegotiationHttpDispatcherExtension`
**Name:** "Dataspace Protocol Negotiation HTTP Dispatcher Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher` (required)
- `org.eclipse.edc.protocol.dsp.http.spi.serialization.JsonLdRemoteMessageSerializer` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspRequestBasePathProvider` (required)

Module `dsp-negotiation-transform-2025`
---------------------------------------
**Artifact:** org.eclipse.edc:dsp-negotiation-transform-2025:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.negotiation.transform.v2025.DspNegotiationTransformV2025Extension`
**Name:** "Dataspace Protocol 2025/1 Negotiation Transform Extension"

**Overview:**  Provides the transformers for DSP v2025/1 negotiation message types via the {@link TypeTransformerRegistry}.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)

Module `dsp-transfer-process-http-api-2025`
-------------------------------------------
**Artifact:** org.eclipse.edc:dsp-transfer-process-http-api-2025:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.transferprocess.http.api.v2025.DspTransferProcessApi2025Extension`
**Name:** "Dataspace Protocol 2025/1: TransferProcess API Extension"

**Overview:**  Creates and registers the controller for dataspace protocol v2025/1 transfer process requests.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService` (required)
- `org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (required)

Module `dsp-transfer-process-http-dispatcher`
---------------------------------------------
**Artifact:** org.eclipse.edc:dsp-transfer-process-http-dispatcher:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.transferprocess.http.dispatcher.DspTransferProcessDispatcherExtension`
**Name:** "Dataspace Protocol Transfer HTTP Dispatcher Extension"

**Overview:**  Provides HTTP dispatching for Dataspace Protocol transfer process messages via the {@link DspHttpRemoteMessageDispatcher}.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher` (required)
- `org.eclipse.edc.protocol.dsp.http.spi.serialization.JsonLdRemoteMessageSerializer` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspRequestBasePathProvider` (required)

Module `dsp-transfer-process-transform-2025`
--------------------------------------------
**Artifact:** org.eclipse.edc:dsp-transfer-process-transform-2025:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.transferprocess.transform.v2025.DspTransferProcessTransformV2025Extension`
**Name:** "Dataspace Protocol 2025/1 Transfer Process Transform Extension"

**Overview:**  Provides the transformers for DSP v2025/1 transferprocess message types via the {@link TypeTransformerRegistry}.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `dsp-version-http-api`
-----------------------------
**Artifact:** org.eclipse.edc:dsp-version-http-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.protocol.dsp.version.http.api.DspVersionApiExtension`
**Name:** "Dataspace Protocol Version Api"

**Overview:**  Provide API for the protocol versions.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionProtocolService` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `edr-cache-api`
----------------------
**Artifact:** org.eclipse.edc:edr-cache-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.api.management.edr.EdrCacheApiExtension`
**Name:** "Management API: EDR cache"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `edr-index-sql`
----------------------
**Artifact:** org.eclipse.edc:edr-index-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.edr.store.index.SqlEndpointDataReferenceEntryIndexExtension`
**Name:** "SQL edr entry store"

**Overview:** No overview provided.


### Configuration

| Key                            | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ------------------------------ | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.edr.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.edr.store.index.sql.schema.EndpointDataReferenceEntryStatements` (optional)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `edr-store-core`
-----------------------
**Artifact:** org.eclipse.edc:edr-store-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.edr.store.EndpointDataReferenceStoreExtension`
**Name:** "Endpoint Data Reference Core Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore`

#### Referenced (injected) services
- `org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex` (required)
- `org.eclipse.edc.edr.spi.store.EndpointDataReferenceCache` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)

#### Class: `org.eclipse.edc.edr.store.EndpointDataReferenceStoreDefaultServicesExtension`
**Name:** "Endpoint Data Reference Core Default Services Extension"

**Overview:** No overview provided.


### Configuration

| Key                  | Required | Type     | Default | Pattern | Min | Max | Description                                                                                        |
| -------------------- | -------- | -------- | ------- | ------- | --- | --- | -------------------------------------------------------------------------------------------------- |
| `edc.edr.vault.path` |          | `string` | ``      |         |     |     | Directory/Path where to store EDRs in the vault for vaults that supports hierarchical structuring. |

#### Provided services
- `org.eclipse.edc.edr.spi.store.EndpointDataReferenceCache`
- `org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex`

#### Referenced (injected) services
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)
- `org.eclipse.edc.spi.security.Vault` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `edr-store-receiver`
---------------------------
**Artifact:** org.eclipse.edc:edr-store-receiver:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.edr.store.receiver.EndpointDataReferenceStoreReceiverExtension`
**Name:** "Endpoint Data Reference Store Receiver Extension"

**Overview:** No overview provided.


### Configuration

| Key                     | Required | Type     | Default | Pattern | Min | Max | Description                                                         |
| ----------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------- |
| `edc.edr.receiver.sync` | `*`      | `string` | `false` |         |     |     | If true the EDR receiver will be registered as synchronous listener |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService` (required)
- `org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)

Module `events-cloud-http`
--------------------------
**Artifact:** org.eclipse.edc:events-cloud-http:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.event.cloud.http.CloudEventsHttpExtension`
**Name:** "Cloud events HTTP"

**Overview:** No overview provided.


### Configuration

| Key                               | Required | Type     | Default | Pattern | Min | Max | Description |
| --------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ----------- |
| `edc.events.cloudevents.endpoint` | `*`      | `string` | ``      |         |     |     |             |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.system.Hostname` (required)

Module `iam-mock`
-----------------
**Artifact:** org.eclipse.edc:iam-mock:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.mock.IamMockExtension`
**Name:** "Mock IAM"

**Overview:**  An IAM provider mock used for testing.



### Configuration

| Key                         | Required | Type     | Default          | Pattern | Min | Max | Description                                                                           |
| --------------------------- | -------- | -------- | ---------------- | ------- | --- | --- | ------------------------------------------------------------------------------------- |
| `edc.mock.region`           | `*`      | `string` | `eu`             |         |     |     | Configures the region to be used in the mock tokens                                   |
| `edc.participant.id`        | `*`      | `string` | ``               |         |     |     | Configures the participant id this runtime is operating on behalf of                  |
| `edc.mock.faulty_client_id` | `*`      | `string` | `faultyClientId` |         |     |     | Configures the faulty participant id that the requests to fail (for testing purposes) |
| `edc.agent.identity.key`    | `*`      | `string` | `client_id`      |         |     |     | The name of the claim key used to determine the participant identity                  |

#### Provided services
- `org.eclipse.edc.spi.iam.IdentityService`
- `org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction`
- `org.eclipse.edc.spi.iam.AudienceResolver`

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig` (required)

Module `identity-did-core`
--------------------------
**Artifact:** org.eclipse.edc:identity-did-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.did.IdentityDidCoreExtension`
**Name:** "Identity Did Core"

**Overview:** No overview provided.


### Configuration

| Key                             | Required | Type     | Default  | Pattern | Min | Max | Description                                           |
| ------------------------------- | -------- | -------- | -------- | ------- | --- | --- | ----------------------------------------------------- |
| `edc.did.resolver.cache.expiry` | `*`      | `string` | `300000` |         |     |     | Expiry time for caching DID Documents in milliseconds |

#### Provided services
- `org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry`
- `org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver`

#### Referenced (injected) services
- `org.eclipse.edc.keys.spi.KeyParserRegistry` (required)
- `java.time.Clock` (required)

Module `identity-did-spi`
-------------------------
**Name:** IAM DID services
**Artifact:** org.eclipse.edc:identity-did-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.iam.did.spi.store.DidStore`
  - `org.eclipse.edc.iam.did.spi.credentials.CredentialsVerifier`
  - `org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver`
  - `org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry`

### Extensions
Module `identity-did-web`
-------------------------
**Artifact:** org.eclipse.edc:identity-did-web:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.did.web.WebDidExtension`
**Name:** "Web DID"

**Overview:**  Initializes support for resolving Web DIDs.



### Configuration

| Key                         | Required | Type     | Default | Pattern | Min | Max | Description |
| --------------------------- | -------- | -------- | ------- | ------- | --- | --- | ----------- |
| `edc.webdid.doh.url`        | `*`      | `string` | ``      |         |     |     |             |
| `edc.iam.did.web.use.https` | `*`      | `string` | ``      |         |     |     |             |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry` (required)
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `jersey-core`
--------------------
**Artifact:** org.eclipse.edc:jersey-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.web.jersey.JerseyExtension`
**Name:** "JerseyExtension"

### Configuration

| Key                         | Required | Type     | Default | Pattern | Min | Max | Description |
| --------------------------- | -------- | -------- | ------- | ------- | --- | --- | ----------- |
| `edc.web.rest.cors.origins` | `*`      | `string` | ``      |         |     |     |             |
| `edc.web.rest.cors.enabled` | `*`      | `string` | ``      |         |     |     |             |
| `edc.web.rest.cors.headers` | `*`      | `string` | ``      |         |     |     |             |
| `edc.web.rest.cors.methods` | `*`      | `string` | ``      |         |     |     |             |

#### Provided services
- `org.eclipse.edc.web.spi.WebService`
- `org.eclipse.edc.web.spi.validation.InterceptorFunctionRegistry`

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebServer` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `jersey-micrometer`
--------------------------
**Artifact:** org.eclipse.edc:jersey-micrometer:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.web.jersey.micrometer.JerseyMicrometerExtension`
**Name:** "JerseyMicrometerExtension"

### Configuration

| Key                          | Required | Type     | Default | Pattern | Min | Max | Description |
| ---------------------------- | -------- | -------- | ------- | ------- | --- | --- | ----------- |
| `edc.metrics.enabled`        | `*`      | `string` | ``      |         |     |     |             |
| `edc.metrics.jersey.enabled` | `*`      | `string` | ``      |         |     |     |             |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `io.micrometer.core.instrument.MeterRegistry` (required)

Module `jetty-core`
-------------------
**Artifact:** org.eclipse.edc:jetty-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.web.jetty.JettyExtension`
**Name:** "JettyExtension"

### Configuration

| Key                                 | Required | Type     | Default    | Pattern | Min | Max | Description                  |
| ----------------------------------- | -------- | -------- | ---------- | ------- | --- | --- | ---------------------------- |
| `edc.web.https.keystore.password`   | `*`      | `string` | `password` |         |     |     | Keystore password            |
| `edc.web.https.keymanager.password` | `*`      | `string` | `password` |         |     |     | Keymanager password          |
| `web.http.port`                     | `*`      | `string` | `8181`     |         |     |     | Port for default api context |
| `web.http.path`                     | `*`      | `string` | `/api`     |         |     |     | Path for default api context |
| `edc.web.https.keystore.path`       |          | `string` | ``         |         |     |     | Keystore path                |
| `edc.web.https.keystore.type`       | `*`      | `string` | `PKCS12`   |         |     |     | Keystore type                |

#### Provided services
- `org.eclipse.edc.web.spi.WebServer`
- `org.eclipse.edc.web.jetty.JettyService`
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry`

#### Referenced (injected) services
_None_

Module `jetty-micrometer`
-------------------------
**Artifact:** org.eclipse.edc:jetty-micrometer:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.web.jetty.micrometer.JettyMicrometerExtension`
**Name:** "Jetty Micrometer Metrics"

**Overview:**  An extension that registers Micrometer {@link JettyConnectionMetrics} into Jetty to
 provide server metrics.



### Configuration

| Key                         | Required | Type     | Default | Pattern | Min | Max | Description |
| --------------------------- | -------- | -------- | ------- | ------- | --- | --- | ----------- |
| `edc.metrics.enabled`       | `*`      | `string` | ``      |         |     |     |             |
| `edc.metrics.jetty.enabled` | `*`      | `string` | ``      |         |     |     |             |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.jetty.JettyService` (required)
- `io.micrometer.core.instrument.MeterRegistry` (required)

Module `json-ld`
----------------
**Artifact:** org.eclipse.edc:json-ld:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.jsonld.JsonLdExtension`
**Name:** "JSON-LD Extension"

**Overview:**  Adds support for working with JSON-LD. Provides an ObjectMapper that works with Jakarta JSON-P
 types through the TypeManager context {@link CoreConstants#JSON_LD} and a registry
 for {@link JsonLdTransformer}s. The module also offers
 functions for working with JSON-LD structures.



### Configuration

| Key                                        | Required | Type      | Default | Pattern | Min | Max | Description                                                                                     |
| ------------------------------------------ | -------- | --------- | ------- | ------- | --- | --- | ----------------------------------------------------------------------------------------------- |
| `edc.jsonld.document.<documentAlias>.path` | `*`      | `string`  | ``      |         |     |     | Path of the JSON-LD document to cache                                                           |
| `edc.jsonld.document.<documentAlias>.url`  | `*`      | `string`  | ``      |         |     |     | URL of the JSON-LD document to cache                                                            |
| `edc.jsonld.http.enabled`                  | `*`      | `string`  | `false` |         |     |     | If set enable http json-ld document resolution                                                  |
| `edc.jsonld.https.enabled`                 | `*`      | `boolean` | `false` |         |     |     | If set enable https json-ld document resolution                                                 |
| `edc.jsonld.vocab.disable`                 | `*`      | `string`  | `false` |         |     |     | If true disable the @vocab context definition. This could be used to avoid api breaking changes |
| `edc.jsonld.prefixes.check`                | `*`      | `boolean` | `true`  |         |     |     | If true a validation on expended object will be made against configured prefixes                |

#### Provided services
- `org.eclipse.edc.jsonld.spi.JsonLd`

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `jti-validation-store-sql`
---------------------------------
**Artifact:** org.eclipse.edc:jti-validation-store-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.jtivalidation.store.sql.SqlJtiValidationStoreExtension`
**Name:** "SQL JTI Validation store"

**Overview:** No overview provided.


### Configuration

| Key                            | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ------------------------------ | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.jti.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.jtivalidation.store.sql.schema.JtiValidationStoreStatements` (optional)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `jwt-signer-spi`
-----------------------
**Name:** Implementation SPI that is used to contribute custom JWSSigners to the JwtGenerationService
**Artifact:** org.eclipse.edc:jwt-signer-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider`

### Extensions
Module `jwt-spi`
----------------
**Name:** JTW services
**Artifact:** org.eclipse.edc:jwt-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.jwt.validation.jti.JtiValidationStore`

### Extensions
Module `management-api-configuration`
-------------------------------------
**Artifact:** org.eclipse.edc:management-api-configuration:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.api.management.configuration.ManagementApiConfigurationExtension`
**Name:** "Management API configuration"

**Overview:**  Configure 'management' api context.



### Configuration

| Key                              | Required | Type     | Default           | Pattern | Min | Max | Description                                                                                                     |
| -------------------------------- | -------- | -------- | ----------------- | ------- | --- | --- | --------------------------------------------------------------------------------------------------------------- |
| `web.http.management.port`       | `*`      | `string` | `8181`            |         |     |     | Port for management api context                                                                                 |
| `web.http.management.path`       | `*`      | `string` | `/api/management` |         |     |     | Path for management api context                                                                                 |
| `edc.management.endpoint`        |          | `string` | ``                |         |     |     | Configures endpoint for reaching the Management API, in the format "<hostname:management.port/management.path>" |
| `edc.management.context.enabled` | `*`      | `string` | `false`           |         |     |     | If set enable the usage of management api JSON-LD context.                                                      |

#### Provided services
- `org.eclipse.edc.web.spi.configuration.context.ManagementApiUrl`

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry` (required)
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.participant.spi.ParticipantIdMapper` (required)
- `org.eclipse.edc.spi.system.Hostname` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)

Module `management-api-schema-validator`
----------------------------------------
**Artifact:** org.eclipse.edc:management-api-schema-validator:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension`
**Name:** "Management API Schema Validator"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)

Module `micrometer-core`
------------------------
**Artifact:** org.eclipse.edc:micrometer-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.metrics.micrometer.MicrometerExtension`
**Name:** "Micrometer Metrics"

**Overview:** No overview provided.


### Configuration

| Key                            | Required | Type     | Default | Pattern | Min | Max | Description |
| ------------------------------ | -------- | -------- | ------- | ------- | --- | --- | ----------- |
| `edc.metrics.enabled`          | `*`      | `string` | ``      |         |     |     |             |
| `edc.metrics.system.enabled`   | `*`      | `string` | ``      |         |     |     |             |
| `edc.metrics.okhttp.enabled`   | `*`      | `string` | ``      |         |     |     |             |
| `edc.metrics.executor.enabled` | `*`      | `string` | ``      |         |     |     |             |

#### Provided services
- `okhttp3.EventListener`
- `org.eclipse.edc.spi.system.ExecutorInstrumentation`
- `io.micrometer.core.instrument.MeterRegistry`

#### Referenced (injected) services
_None_

Module `monitor-jdk-logger`
---------------------------
**Artifact:** org.eclipse.edc:monitor-jdk-logger:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.monitor.logger.LoggerMonitorExtension`
**Name:** "Logger monitor"

**Overview:**  Extension adding logging monitor.

 @deprecated will be removed soon.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
_None_

Module `oauth2-client`
----------------------
**Artifact:** org.eclipse.edc:oauth2-client:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.oauth2.client.Oauth2ClientExtension`
**Name:** "OAuth2 Client"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client`

#### Referenced (injected) services
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `oauth2-spi`
-------------------
**Name:** OAuth2 services
**Artifact:** org.eclipse.edc:oauth2-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client`

### Extensions
Module `participant-context-config-core`
----------------------------------------
**Artifact:** org.eclipse.edc:participant-context-config-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.participantcontext.config.defaults.ParticipantContextConfigDefaultServicesExtension`
**Name:** "Participant Context Config Default Services Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore`

#### Referenced (injected) services
_None_

#### Class: `org.eclipse.edc.participantcontext.config.ParticipantContextConfigServicesExtension`
**Name:** "Participant Context Config Services Extension"

**Overview:** No overview provided.


### Configuration

| Key                                            | Required | Type     | Default | Pattern | Min | Max | Description                                                                   |
| ---------------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ----------------------------------------------------------------------------- |
| `edc.participants.config.encryption.algorithm` | `*`      | `string` | `aes`   |         |     |     | The encryption algorithm used for encrypting and decrypting sensitive config. |

#### Provided services
- `org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService`
- `org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig`

#### Referenced (injected) services
- `org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.encryption.EncryptionAlgorithmRegistry` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `participant-context-core`
---------------------------------
**Artifact:** org.eclipse.edc:participant-context-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.participantcontext.ParticipantContextDefaultServicesExtension`
**Name:** "Participant Context Default Services Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore`

#### Referenced (injected) services
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

#### Class: `org.eclipse.edc.participantcontext.ParticipantContextServicesExtension`
**Name:** "Participant Context Default Services Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.participantcontext.spi.service.ParticipantContextService`

#### Referenced (injected) services
- `org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)

Module `participant-context-single-core`
----------------------------------------
**Artifact:** org.eclipse.edc:participant-context-single-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.participantcontext.single.SingleParticipantContextDefaultServicesExtension`
**Name:** "Single Participant Context Default Services Extension"

**Overview:** No overview provided.


### Configuration

| Key                          | Required | Type     | Default     | Pattern | Min | Max | Description                                                              |
| ---------------------------- | -------- | -------- | ----------- | ------- | --- | --- | ------------------------------------------------------------------------ |
| `edc.participant.id`         | `*`      | `string` | `anonymous` |         |     |     | Configures the participant id this runtime is operating on behalf of     |
| `edc.participant.context.id` |          | `string` | ``          |         |     |     | Configures the participant context id for the single participant runtime |

#### Provided services
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier`
- `org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore`
- `org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver`

#### Referenced (injected) services
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `participantcontext-config-store-sql`
--------------------------------------------
**Artifact:** org.eclipse.edc:participantcontext-config-store-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.store.sql.participantcontext.config.SqlParticipantContextConfigStoreExtension`
**Name:** "ParticipantContextConfig SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                                 | Required | Type     | Default   | Pattern | Min | Max | Description               |
| --------------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.participantcontextconfig.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.connector.store.sql.participantcontext.config.ParticipantContextConfigStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `participantcontext-store-sql`
-------------------------------------
**Artifact:** org.eclipse.edc:participantcontext-store-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.store.sql.participantcontext.SqlParticipantContextStoreExtension`
**Name:** "ParticipantContext SQL Store Extension"

**Overview:** No overview provided.


### Configuration

| Key                                           | Required | Type     | Default   | Pattern | Min | Max | Description               |
| --------------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.participantcontext.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.connector.store.sql.participantcontext.ParticipantContextStoreStatements` (optional)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `policy-definition-api`
------------------------------
**Artifact:** org.eclipse.edc:policy-definition-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.api.management.policy.PolicyDefinitionApiExtension`
**Name:** "Management API: Policy Definition"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (required)

Module `policy-definition-store-sql`
------------------------------------
**Artifact:** org.eclipse.edc:policy-definition-store-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.schema.SqlPolicyStoreStatements`

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.store.sql.policydefinition.SqlPolicyStoreExtension`
**Name:** "SQL policy store"

**Overview:** No overview provided.


### Configuration

| Key                               | Required | Type     | Default   | Pattern | Min | Max | Description               |
| --------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.policy.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.schema.SqlPolicyStoreStatements` (optional)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)

Module `policy-engine-spi`
--------------------------
**Name:** Policy Engine services
**Artifact:** org.eclipse.edc:policy-engine-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.policy.engine.spi.RuleBindingRegistry`
  - `org.eclipse.edc.policy.engine.spi.PolicyEngine`

### Extensions
Module `policy-monitor-core`
----------------------------
**Artifact:** org.eclipse.edc:policy-monitor-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.policy.monitor.PolicyMonitorExtension`
**Name:** "Policy Monitor"

**Overview:** No overview provided.


### Configuration

| Key                                   | Required | Type     | Default | Pattern | Min | Max | Description                                                                 |
| ------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | --------------------------------------------------------------------------- |
| `state-machine.iteration-wait-millis` | `*`      | `string` | `1000`  |         |     |     | The iteration wait time in milliseconds in the state machine.               |
| `state-machine.batch-size`            | `*`      | `string` | `20`    |         |     |     | The number of entities to be processed on every iteration.                  |
| `send.retry.limit`                    | `*`      | `string` | `7`     |         |     |     | How many times a specific operation must be tried before failing with error |
| `send.retry.base-delay.ms`            | `*`      | `string` | `1000`  |         |     |     | The base delay for the consumer negotiation retry mechanism in millisecond  |

#### Provided services
- `org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorManager`

#### Referenced (injected) services
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.spi.telemetry.Telemetry` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService` (required)
- `org.eclipse.edc.policy.engine.spi.PolicyEngine` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService` (required)
- `org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore` (required)
- `org.eclipse.edc.policy.engine.spi.RuleBindingRegistry` (required)

#### Class: `org.eclipse.edc.connector.policy.monitor.PolicyMonitorDefaultServicesExtension`
**Name:** "PolicyMonitor Default Services"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore`

#### Referenced (injected) services
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry` (required)

Module `policy-monitor-store-sql`
---------------------------------
**Artifact:** org.eclipse.edc:policy-monitor-store-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.policy.monitor.store.sql.SqlPolicyMonitorStoreExtension`
**Name:** "SqlPolicyMonitorStoreExtension"

### Configuration

| Key                                       | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ----------------------------------------- | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.policy-monitor.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.connector.policy.monitor.store.sql.schema.PolicyMonitorStatements` (optional)
- `java.time.Clock` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider` (required)

Module `policy-spi`
-------------------
**Name:** Policy services
**Artifact:** org.eclipse.edc:policy-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive`
  - `org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore`

### Extensions
Module `runtime-core`
---------------------
**Artifact:** org.eclipse.edc:runtime-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.runtime.core.RuntimeCoreServicesExtension`
**Name:** "Runtime Default Core Services"

**Overview:** No overview provided.


### Configuration

| Key                     | Required | Type     | Default     | Pattern | Min | Max | Description                                                            |
| ----------------------- | -------- | -------- | ----------- | ------- | --- | --- | ---------------------------------------------------------------------- |
| `edc.hostname`          | `*`      | `string` | `localhost` |         |     |     | Runtime hostname, which e.g. is used in referer urls                   |
| `edc.encryption.strict` | `*`      | `string` | `true`      |         |     |     | Whether to fail when an unsupported encryption algorithm is requested. |

#### Provided services
- `org.eclipse.edc.spi.types.TypeManager`
- `org.eclipse.edc.spi.system.Hostname`
- `org.eclipse.edc.http.spi.EdcHttpClient`
- `org.eclipse.edc.spi.command.CommandHandlerRegistry`
- `org.eclipse.edc.spi.event.EventRouter`
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry`
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry`
- `org.eclipse.edc.spi.query.CriterionOperatorRegistry`
- `org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry`
- `org.eclipse.edc.encryption.EncryptionAlgorithmRegistry`

#### Referenced (injected) services
- `okhttp3.OkHttpClient` (required)
- `dev.failsafe.RetryPolicy<okhttp3.Response>` (required)
- `java.time.Clock` (required)

#### Class: `org.eclipse.edc.runtime.core.RuntimeDefaultCoreServicesExtension`
**Name:** "Runtime Default Core Services"

**Overview:** No overview provided.


### Configuration

| Key                                      | Required | Type     | Default | Pattern | Min | Max | Description                                                         |
| ---------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------------------------------------- |
| `edc.core.retry.retries.max`             | `*`      | `string` | `5`     |         |     |     | RetryPolicy: Maximum retries before a failure is propagated         |
| `edc.core.retry.backoff.min`             | `*`      | `string` | `500`   |         |     |     | RetryPolicy: Minimum number of milliseconds for exponential backoff |
| `edc.core.retry.backoff.max`             | `*`      | `string` | `10000` |         |     |     | RetryPolicy: Maximum number of milliseconds for exponential backoff |
| `edc.core.retry.log.on.retry`            | `*`      | `string` | `false` |         |     |     | RetryPolicy: Log onRetry events                                     |
| `edc.core.retry.log.on.retry.scheduled`  | `*`      | `string` | `false` |         |     |     | RetryPolicy: Log onRetryScheduled events                            |
| `edc.core.retry.log.on.retries.exceeded` | `*`      | `string` | `false` |         |     |     | RetryPolicy: Log onRetriesExceeded events                           |
| `edc.core.retry.log.on.failed.attempt`   | `*`      | `string` | `false` |         |     |     | RetryPolicy: Log onFailedAttempt events                             |
| `edc.core.retry.log.on.abort`            | `*`      | `string` | `false` |         |     |     | RetryPolicy: Log onAbort events                                     |
| `edc.http.client.https.enforce`          | `*`      | `string` | `false` |         |     |     | OkHttpClient: If true, enable HTTPS call enforcement                |
| `edc.http.client.timeout.connect`        | `*`      | `string` | `30`    |         |     |     | OkHttpClient: connect timeout, in seconds                           |
| `edc.http.client.timeout.read`           | `*`      | `string` | `30`    |         |     |     | OkHttpClient: read timeout, in seconds                              |
| `edc.http.client.send.buffer.size`       | `*`      | `string` | `0`     |         |     |     | OkHttpClient: send buffer size, in bytes                            |
| `edc.http.client.receive.buffer.size`    | `*`      | `string` | `0`     |         |     |     | OkHttpClient: receive buffer size, in bytes                         |

#### Provided services
- `org.eclipse.edc.transaction.spi.TransactionContext`
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry`
- `dev.failsafe.RetryPolicy<T>`
- `okhttp3.OkHttpClient`

#### Referenced (injected) services
- `okhttp3.EventListener` (optional)

Module `secrets-api`
--------------------
**Artifact:** org.eclipse.edc:secrets-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.api.management.secret.SecretsApiExtension`
**Name:** "Management API: Secret"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.connector.spi.service.SecretService` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)

Module `signaling-data-plane`
-----------------------------
**Artifact:** org.eclipse.edc:signaling-data-plane:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.test.runtime.signaling.SignalingDataPlaneRuntimeExtension`
**Name:** "SignalingDataPlaneRuntimeExtension"

### Configuration

| Key                                         | Required | Type     | Default | Pattern | Min | Max | Description |
| ------------------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ----------- |
| `signaling.dataplane.controlplane.endpoint` | `*`      | `string` | ``      |         |     |     |             |
| `web.http.port`                             | `*`      | `string` | ``      |         |     |     |             |
| `web.http.path`                             | `*`      | `string` | ``      |         |     |     |             |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `sql-bootstrapper`
-------------------------
**Artifact:** org.eclipse.edc:sql-bootstrapper:0.16.0-SNAPSHOT

**Categories:** _sql, persistence, storage, sql, persistence, storage_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapperExtension`
**Name:** "SQL Schema Bootstrapper Extension"

**Overview:** No overview provided.


### Configuration

| Key                         | Required | Type     | Default | Pattern | Min | Max | Description                                                                                         |
| --------------------------- | -------- | -------- | ------- | ------- | --- | --- | --------------------------------------------------------------------------------------------------- |
| `edc.sql.schema.autocreate` |          | `string` | `false` |         |     |     | When true, the schema for the sql stores will be created automatically on the configured datasource |

#### Provided services
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

Module `sql-core`
-----------------
**Artifact:** org.eclipse.edc:sql-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.sql.SqlCoreExtension`
**Name:** "SQL Core"

**Overview:** No overview provided.


### Configuration

| Key                  | Required | Type     | Default | Pattern | Min | Max | Description                          |
| -------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------------------ |
| `edc.sql.fetch.size` | `*`      | `string` | `5000`  |         |     |     | Fetch size value used in SQL queries |

#### Provided services
- `org.eclipse.edc.sql.QueryExecutor`
- `org.eclipse.edc.sql.ConnectionFactory`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)

Module `sql-lease-core`
-----------------------
**Artifact:** org.eclipse.edc:sql-lease-core:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.sql.lease.SqlLeaseCoreExtension`
**Name:** "SQL Lease Core"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.sql.lease.spi.LeaseStatements` (optional)

Module `sql-pool-apache-commons`
--------------------------------
**Artifact:** org.eclipse.edc:sql-pool-apache-commons:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.sql.pool.commons.CommonsConnectionPoolServiceExtension`
**Name:** "Commons Connection Pool"

**Overview:** No overview provided.


### Configuration

| Key                                                    | Required | Type      | Default | Pattern | Min | Max | Description                                                                                                                 |
| ------------------------------------------------------ | -------- | --------- | ------- | ------- | --- | --- | --------------------------------------------------------------------------------------------------------------------------- |
| `edc.datasource.<name>url`                             | `*`      | `string`  | ``      |         |     |     | JDBC url                                                                                                                    |
| `edc.datasource.<name>user`                            | `*`      | `string`  | ``      |         |     |     | Username to be used for the JDBC connection. Can be omitted if not required, or if the user is encoded in the JDBC url.     |
| `edc.datasource.<name>password`                        | `*`      | `string`  | ``      |         |     |     | Username to be used for the JDBC connection. Can be omitted if not required, or if the password is encoded in the JDBC url. |
| `edc.datasource.<name>pool.connections.max-idle`       | `*`      | `int`     | ``      |         |     |     | Pool max idle connections                                                                                                   |
| `edc.datasource.<name>pool.connections.max-total`      | `*`      | `int`     | ``      |         |     |     | Pool max total connections                                                                                                  |
| `edc.datasource.<name>pool.connections.min-idle`       | `*`      | `int`     | ``      |         |     |     | Pool min idle connections                                                                                                   |
| `edc.datasource.<name>pool.connection.test.on-borrow`  | `*`      | `boolean` | ``      |         |     |     | Pool test on borrow                                                                                                         |
| `edc.datasource.<name>pool.connection.test.on-create`  | `*`      | `boolean` | ``      |         |     |     | Pool test on create                                                                                                         |
| `edc.datasource.<name>pool.connection.test.on-return`  | `*`      | `boolean` | ``      |         |     |     | Pool test on return                                                                                                         |
| `edc.datasource.<name>pool.connection.test.while-idle` | `*`      | `boolean` | ``      |         |     |     | Pool test while idle                                                                                                        |
| `edc.datasource.<name>pool.connection.test.query`      | `*`      | `string`  | ``      |         |     |     | Pool test query                                                                                                             |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.sql.ConnectionFactory` (required)
- `org.eclipse.edc.spi.security.Vault` (required)

Module `tck-extension`
----------------------
**Artifact:** org.eclipse.edc:tck-extension:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.tck.dsp.setup.TckSetupExtension`
**Name:** "DSP TCK Setup"

**Overview:**  Loads customizations and seed data for the TCK.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService` (required)
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore` (required)

#### Class: `org.eclipse.edc.tck.dsp.guard.TckGuardExtension`
**Name:** "TckGuardExtension"

**Overview:**  Loads customizations and seed data for the TCK.



### Configuration_None_

#### Provided services
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard`
- `org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard`

#### Referenced (injected) services
- `org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore` (required)
- `org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `org.eclipse.edc.spi.event.EventRouter` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)

#### Class: `org.eclipse.edc.tck.dsp.transfer.TckDataPlaneExtension`
**Name:** "TckDataPlaneExtension"

**Overview:**  Loads customizations and seed data for the TCK.



### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService` (required)
- `org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService` (required)
- `org.eclipse.edc.spi.security.Vault` (required)

#### Class: `org.eclipse.edc.tck.dsp.identity.TckIdentityExtension`
**Name:** "TckIdentityExtension"

**Overview:**  Loads customizations and seed data for the TCK.



### Configuration_None_

#### Provided services
- `org.eclipse.edc.spi.iam.IdentityService`
- `org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction`
- `org.eclipse.edc.spi.iam.AudienceResolver`

#### Referenced (injected) services
_None_

#### Class: `org.eclipse.edc.tck.dsp.controller.TckControllerExtension`
**Name:** "TckControllerExtension"

**Overview:**  Loads customizations and seed data for the TCK.



### Configuration

| Key                 | Required | Type     | Default | Pattern | Min | Max | Description              |
| ------------------- | -------- | -------- | ------- | ------- | --- | --- | ------------------------ |
| `web.http.tck.port` | `*`      | `string` | `8687`  |         |     |     | Port for tck api context |
| `web.http.tck.path` | `*`      | `string` | `/tck`  |         |     |     | Path for tck api context |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.configuration.PortMappingRegistry` (required)
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.web.spi.WebServer` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService` (required)
- `org.eclipse.edc.spi.monitor.Monitor` (required)
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (required)

Module `token-core`
-------------------
**Artifact:** org.eclipse.edc:token-core:0.16.0-SNAPSHOT

**Categories:** _token, security, auth, token, security, auth_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.token.TokenServicesExtension`
**Name:** "Token Services Extension"

**Overview:**  This extension registers the {@link TokenValidationService} and the {@link TokenValidationRulesRegistry}
 which can then be used by downstream modules.



### Configuration_None_

#### Provided services
- `org.eclipse.edc.token.spi.TokenValidationRulesRegistry`
- `org.eclipse.edc.token.spi.TokenValidationService`
- `org.eclipse.edc.token.spi.TokenDecoratorRegistry`
- `org.eclipse.edc.jwt.signer.spi.JwsSignerProvider`
- `org.eclipse.edc.jwt.validation.jti.JtiValidationStore`

#### Referenced (injected) services
- `org.eclipse.edc.keys.spi.PrivateKeyResolver` (required)

Module `token-spi`
------------------
**Name:** Token services
**Artifact:** org.eclipse.edc:token-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
Module `transaction-atomikos`
-----------------------------
**Artifact:** org.eclipse.edc:transaction-atomikos:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.transaction.atomikos.AtomikosTransactionExtension`
**Name:** "Atomikos Transaction"

**Overview:**  Provides an implementation of a {@link DataSourceRegistry} and a {@link TransactionContext} backed by Atomikos.



### Configuration

| Key                                | Required | Type     | Default | Pattern | Min | Max | Description |
| ---------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ----------- |
| `driver.class`                     | `*`      | `string` | ``      |         |     |     |             |
| `url`                              | `*`      | `string` | ``      |         |     |     |             |
| `type`                             | `*`      | `string` | ``      |         |     |     |             |
| `username`                         | `*`      | `string` | ``      |         |     |     |             |
| `password`                         | `*`      | `string` | ``      |         |     |     |             |
| `pool.size`                        | `*`      | `string` | ``      |         |     |     |             |
| `max.pool.size`                    | `*`      | `string` | ``      |         |     |     |             |
| `min.pool.size`                    | `*`      | `string` | ``      |         |     |     |             |
| `connection.timeout`               | `*`      | `string` | ``      |         |     |     |             |
| `login.timeout`                    | `*`      | `string` | ``      |         |     |     |             |
| `maintenance.interval`             | `*`      | `string` | ``      |         |     |     |             |
| `max.idle`                         | `*`      | `string` | ``      |         |     |     |             |
| `query`                            | `*`      | `string` | ``      |         |     |     |             |
| `properties`                       | `*`      | `string` | ``      |         |     |     |             |
| `edc.atomikos.timeout`             |          | `string` | ``      |         |     |     |             |
| `edc.atomikos.directory`           |          | `string` | ``      |         |     |     |             |
| `edc.atomikos.threaded2pc`         |          | `string` | ``      |         |     |     |             |
| `edc.atomikos.logging`             |          | `string` | ``      |         |     |     |             |
| `edc.atomikos.checkpoint.interval` |          | `string` | ``      |         |     |     |             |

#### Provided services
- `org.eclipse.edc.transaction.spi.TransactionContext`
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry`

#### Referenced (injected) services
_None_

Module `transaction-datasource-spi`
-----------------------------------
**Name:** DataSource services
**Artifact:** org.eclipse.edc:transaction-datasource-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry`

### Extensions
Module `transaction-local`
--------------------------
**Artifact:** org.eclipse.edc:transaction-local:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.transaction.local.LocalTransactionExtension`
**Name:** "Local Transaction"

**Overview:**  Support for transaction context backed by one or more local resources, including a {@link DataSourceRegistry}.



### Configuration_None_

#### Provided services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry`
- `org.eclipse.edc.transaction.spi.TransactionContext`

#### Referenced (injected) services
_None_

Module `transaction-spi`
------------------------
**Name:** Transactional context services
**Artifact:** org.eclipse.edc:transaction-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.transaction.spi.TransactionContext`

### Extensions
Module `transfer-data-plane-signaling`
--------------------------------------
**Artifact:** org.eclipse.edc:transfer-data-plane-signaling:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.transfer.dataplane.TransferDataPlaneSignalingExtension`
**Name:** "Legacy Data Plane Signaling Extension"

**Overview:** No overview provided.


### Configuration

| Key                                      | Required | Type     | Default  | Pattern | Min | Max | Description                                                                                              |
| ---------------------------------------- | -------- | -------- | -------- | ------- | --- | --- | -------------------------------------------------------------------------------------------------------- |
| `edc.dataplane.client.selector.strategy` | `*`      | `string` | `random` |         |     |     | Defines strategy for Data Plane instance selection in case Data Plane is not embedded in current runtime |

#### Provided services
- `org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController`

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.configuration.context.ControlApiUrl` (optional)
- `org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService` (required)
- `org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory` (required)
- `org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider` (optional)
- `org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser` (required)
- `org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex` (required)

Module `transfer-process-api`
-----------------------------
**Artifact:** org.eclipse.edc:transfer-process-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.api.management.transferprocess.TransferProcessApiExtension`
**Name:** "Management API: Transfer Process"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.transform.spi.TypeTransformerRegistry` (required)
- `org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService` (required)
- `org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry` (required)
- `org.eclipse.edc.jsonld.spi.JsonLd` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier` (required)

Module `transfer-process-store-sql`
-----------------------------------
**Artifact:** org.eclipse.edc:transfer-process-store-sql:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.TransferProcessStoreStatements`

### Extensions
#### Class: `org.eclipse.edc.connector.controlplane.store.sql.transferprocess.SqlTransferProcessStoreExtension`
**Name:** "SQL transfer process store"

**Overview:** No overview provided.


### Configuration

| Key                                        | Required | Type     | Default   | Pattern | Min | Max | Description               |
| ------------------------------------------ | -------- | -------- | --------- | ------- | --- | --- | ------------------------- |
| `edc.sql.store.transferprocess.datasource` | `*`      | `string` | `default` |         |     |     | The datasource to be used |

#### Provided services
- `org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore`

#### Referenced (injected) services
- `org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry` (required)
- `org.eclipse.edc.transaction.spi.TransactionContext` (required)
- `java.time.Clock` (required)
- `org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.TransferProcessStoreStatements` (optional)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.sql.QueryExecutor` (required)
- `org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper` (required)
- `org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider` (required)

Module `transfer-spi`
---------------------
**Name:** Transfer services
**Artifact:** org.eclipse.edc:transfer-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider`
  - `org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard`
  - `org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager`
  - `org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable`
  - `org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore`

### Extensions
Module `validator-data-address-http-data`
-----------------------------------------
**Artifact:** org.eclipse.edc:validator-data-address-http-data:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.validator.dataaddress.httpdata.HttpDataDataAddressValidatorExtension`
**Name:** "DataAddress HttpData Validator"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.validator.spi.DataAddressValidatorRegistry` (required)

Module `validator-data-address-kafka`
-------------------------------------
**Artifact:** org.eclipse.edc:validator-data-address-kafka:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.validator.dataaddress.kafka.KafkaDataAddressValidatorExtension`
**Name:** "DataAddress Kafka Validator"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.validator.spi.DataAddressValidatorRegistry` (required)

Module `vault-hashicorp`
------------------------
**Artifact:** org.eclipse.edc:vault-hashicorp:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.vault.hashicorp.HashicorpVaultExtension`
**Name:** "Hashicorp Vault"

**Overview:** No overview provided.


### Configuration

| Key                                                 | Required | Type     | Default          | Pattern | Min | Max | Description                                                                                                                        |
| --------------------------------------------------- | -------- | -------- | ---------------- | ------- | --- | --- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `edc.vault.hashicorp.url`                           | `*`      | `string` | ``               |         |     |     | The URL of the Hashicorp Vault                                                                                                     |
| `edc.vault.hashicorp.health.check.enabled`          | `*`      | `string` | `true`           |         |     |     | Whether or not the vault health check is enabled                                                                                   |
| `edc.vault.hashicorp.api.health.check.path`         | `*`      | `string` | `/v1/sys/health` |         |     |     | The URL path of the vault's /health endpoint                                                                                       |
| `edc.vault.hashicorp.health.check.standby.ok`       | `*`      | `string` | `false`          |         |     |     | Specifies if being a standby should still return the active status code instead of the standby status code                         |
| `edc.vault.hashicorp.token.scheduled-renew-enabled` | `*`      | `string` | `true`           |         |     |     | Whether the automatic token renewal process will be triggered or not. Should be disabled only for development and testing purposes |
| `edc.vault.hashicorp.token.ttl`                     | `*`      | `string` | `300`            |         |     |     | The time-to-live (ttl) value of the Hashicorp Vault token in seconds                                                               |
| `edc.vault.hashicorp.token.renew-buffer`            | `*`      | `string` | `30`             |         |     |     | The renew buffer of the Hashicorp Vault token in seconds                                                                           |
| `edc.vault.hashicorp.api.secret.path`               | `*`      | `string` | `/v1/secret`     |         |     |     | The URL path of the vault's /secret endpoint                                                                                       |
| `edc.vault.hashicorp.folder`                        |          | `string` | ``               |         |     |     | The path of the folder that the secret is stored in, relative to VAULT_FOLDER_PATH                                                 |
| `edc.vault.hashicorp.allow-fallback`                | `*`      | `string` | `true`           |         |     |     | Allow fallback to default vault partition if vault partitioning is not set up                                                      |

#### Provided services
- `org.eclipse.edc.spi.security.Vault`
- `org.eclipse.edc.spi.security.SignatureService`
- `@org.jetbrains.annotations.NotNull org.eclipse.edc.vault.hashicorp.client.HashicorpVaultHealthService`

#### Referenced (injected) services
- `org.eclipse.edc.http.spi.EdcHttpClient` (required)
- `org.eclipse.edc.spi.system.ExecutorInstrumentation` (required)
- `org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider` (required)
- `org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig` (required)

#### Class: `org.eclipse.edc.vault.hashicorp.health.HashicorpVaultHealthExtension`
**Name:** "Hashicorp Vault Health"

**Overview:** No overview provided.


### Configuration

| Key                                                 | Required | Type     | Default          | Pattern | Min | Max | Description                                                                                                                        |
| --------------------------------------------------- | -------- | -------- | ---------------- | ------- | --- | --- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `edc.vault.hashicorp.url`                           | `*`      | `string` | ``               |         |     |     | The URL of the Hashicorp Vault                                                                                                     |
| `edc.vault.hashicorp.health.check.enabled`          | `*`      | `string` | `true`           |         |     |     | Whether or not the vault health check is enabled                                                                                   |
| `edc.vault.hashicorp.api.health.check.path`         | `*`      | `string` | `/v1/sys/health` |         |     |     | The URL path of the vault's /health endpoint                                                                                       |
| `edc.vault.hashicorp.health.check.standby.ok`       | `*`      | `string` | `false`          |         |     |     | Specifies if being a standby should still return the active status code instead of the standby status code                         |
| `edc.vault.hashicorp.token.scheduled-renew-enabled` | `*`      | `string` | `true`           |         |     |     | Whether the automatic token renewal process will be triggered or not. Should be disabled only for development and testing purposes |
| `edc.vault.hashicorp.token.ttl`                     | `*`      | `string` | `300`            |         |     |     | The time-to-live (ttl) value of the Hashicorp Vault token in seconds                                                               |
| `edc.vault.hashicorp.token.renew-buffer`            | `*`      | `string` | `30`             |         |     |     | The renew buffer of the Hashicorp Vault token in seconds                                                                           |
| `edc.vault.hashicorp.api.secret.path`               | `*`      | `string` | `/v1/secret`     |         |     |     | The URL path of the vault's /secret endpoint                                                                                       |
| `edc.vault.hashicorp.folder`                        |          | `string` | ``               |         |     |     | The path of the folder that the secret is stored in, relative to VAULT_FOLDER_PATH                                                 |
| `edc.vault.hashicorp.allow-fallback`                | `*`      | `string` | `true`           |         |     |     | Allow fallback to default vault partition if vault partitioning is not set up                                                      |

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.spi.system.health.HealthCheckService` (required)
- `org.eclipse.edc.vault.hashicorp.client.HashicorpVaultHealthService` (required)

#### Class: `org.eclipse.edc.vault.hashicorp.auth.HashicorpVaultAuthenticationExtension`
**Name:** "Hashicorp Vault Authentication"

**Overview:** No overview provided.


### Configuration

| Key                                | Required | Type     | Default | Pattern | Min | Max | Description                                                                                           |
| ---------------------------------- | -------- | -------- | ------- | ------- | --- | --- | ----------------------------------------------------------------------------------------------------- |
| `edc.vault.hashicorp.token`        |          | `string` | ``      |         |     |     | The token used to access the Hashicorp Vault. Only required, if default token authentication is used. |
| `edc.vault.hashicorp.clientid`     |          | `string` | ``      |         |     |     | Client-ID to use when obtaining an OAuth2 JWT for Vault access                                        |
| `edc.vault.hashicorp.clientsecret` |          | `string` | ``      |         |     |     | Client-Secret to use when obtaining an OAuth2 JWT for Vault access                                    |
| `edc.vault.hashicorp.tokenurl`     |          | `string` | ``      |         |     |     | URL of the OAuth2 token endpoint                                                                      |

#### Provided services
- `org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider`

#### Referenced (injected) services
- `org.eclipse.edc.http.spi.EdcHttpClient` (optional)

Module `verifiable-credentials`
-------------------------------
**Artifact:** org.eclipse.edc:verifiable-credentials:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.iam.verifiablecredentials.RevocationServiceRegistryExtension`
**Name:** "Revocation Service Extension"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
- `org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry`

#### Referenced (injected) services
_None_

Module `version-api`
--------------------
**Artifact:** org.eclipse.edc:version-api:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
_None_

### Extensions
#### Class: `org.eclipse.edc.connector.api.management.version.VersionApiExtension`
**Name:** "Management API: Version Information"

**Overview:** No overview provided.


### Configuration_None_

#### Provided services
_None_

#### Referenced (injected) services
- `org.eclipse.edc.web.spi.WebService` (required)
- `org.eclipse.edc.spi.types.TypeManager` (required)
- `org.eclipse.edc.spi.system.apiversion.ApiVersionService` (required)

Module `web-spi`
----------------
**Name:** Web services
**Artifact:** org.eclipse.edc:web-spi:0.16.0-SNAPSHOT

**Categories:** _None_

### Extension points
  - `org.eclipse.edc.web.spi.WebService`
  - `org.eclipse.edc.web.spi.WebServer`
  - `org.eclipse.edc.web.spi.validation.InterceptorFunctionRegistry`

### Extensions
