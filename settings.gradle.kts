/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering
 *       ZF Friedrichshafen AG - add dependency & reorder entries
 *
 */

rootProject.name = "dataspaceconnector"

// modules for common/util code

include(":common:util")
include(":common:state-machine-lib")
include(":common:token-generation-lib")
include(":common:token-validation-lib")


// EDC core modules
include(":core")
include(":core:policy:policy-engine")
include(":core:policy:policy-evaluator")
include(":core:transfer")
include(":core:contract")
include(":core:base")
include(":core:boot")
include(":core:micrometer")

// modules that provide implementations for data ingress/egress
include(":data-protocols:ids:ids-api-multipart-endpoint-v1")
include(":data-protocols:ids:ids-api-multipart-dispatcher-v1")
include(":data-protocols:ids:ids-core")
include(":data-protocols:ids:ids-spi")
include(":data-protocols:ids:ids-transform-v1")
include(":data-protocols:ids:ids-token-validation")
include(":data-protocols:ids:ids-api-configuration")

include("extensions:sql:contract-definition:store")

// modules for technology- or cloud-provider extensions
include(":extensions:aws")
include(":extensions:api:control")
include(":extensions:api:api-core")
include(":extensions:api:auth-spi")
include(":extensions:api:auth-tokenbased")
include(":extensions:api:data-management:api-configuration")
include(":extensions:api:data-management:asset")
include(":extensions:api:data-management:contractdefinition")
include(":extensions:api:data-management:contractnegotiation")
include(":extensions:api:data-management:contractagreement")
include(":extensions:api:data-management:policydefinition")
include(":extensions:api:data-management:transferprocess")
include(":extensions:api:observability")
include(":extensions:aws:s3:s3-core")
include(":extensions:aws:s3:s3-provision")
include(":extensions:aws:s3:s3-data-operator")
include(":extensions:aws:aws-test")
include(":extensions:azure:blobstorage")
include(":extensions:azure:blobstorage:blob-core")
include(":extensions:azure:blobstorage:blob-provision")
include(":extensions:azure:blobstorage:blob-data-operator")
include(":extensions:azure:events")
include(":extensions:azure:events-config")
include(":extensions:azure:azure-test")
include(":extensions:azure:cosmos:transfer-process-store-cosmos")
include(":extensions:azure:cosmos:fcc-node-directory-cosmos")
include(":extensions:azure:cosmos:contract-definition-store-cosmos")
include(":extensions:azure:cosmos:contract-negotiation-store-cosmos")
include(":extensions:azure:cosmos:cosmos-common")
include(":extensions:azure:cosmos:assetindex-cosmos")
include(":extensions:azure:vault")
include(":extensions:filesystem:configuration-fs")
include(":extensions:filesystem:vault-fs")
include(":extensions:in-memory:assetindex-memory")
include(":extensions:in-memory:transfer-store-memory")
include(":extensions:in-memory:did-document-store-inmem")
include(":extensions:in-memory:identity-hub-memory")
include(":extensions:in-memory:fcc-node-directory-memory")
include(":extensions:in-memory:fcc-store-memory")
include(":extensions:in-memory:negotiation-store-memory")
include(":extensions:in-memory:contractdefinition-store-memory")
include(":extensions:iam:iam-mock")
include(":extensions:iam:oauth2:oauth2-spi")
include(":extensions:iam:oauth2:oauth2-core")
include(":extensions:iam:daps")
include(":extensions:iam:decentralized-identity")
include(":extensions:iam:decentralized-identity:identity-did-spi")
include(":extensions:iam:decentralized-identity:identity-did-core")
include(":extensions:iam:decentralized-identity:identity-did-service")
include(":extensions:iam:decentralized-identity:identity-did-web")
include(":extensions:iam:decentralized-identity:identity-did-crypto")
include(":extensions:iam:decentralized-identity:registration-service")
include(":extensions:iam:decentralized-identity:registration-service-api")
include(":extensions:iam:decentralized-identity:identity-common-test")
include(":extensions:iam:decentralized-identity:dummy-credentials-verifier")
include(":extensions:catalog:federated-catalog-cache")
include(":extensions:catalog:federated-catalog-spi")
include(":extensions:transfer-functions:transfer-functions-spi")
include(":extensions:transfer-functions:transfer-functions-core")
include(":extensions:dataloading")
include(":extensions:policy:ids-policy")
include(":extensions:jdk-logger-monitor")
include(":extensions:http")
include(":extensions:http:jersey")
include(":extensions:http:jersey-micrometer")
include(":extensions:http:jetty")
include(":extensions:http:jetty-micrometer")
include(":extensions:transaction")
include(":extensions:transaction:transaction-spi")
include(":extensions:transaction:transaction-datasource-spi")
include(":extensions:transaction:transaction-atomikos")
include(":extensions:transaction:transaction-local")
include(":extensions:data-plane-transfer:data-plane-transfer-spi")
include(":extensions:data-plane-transfer:data-plane-transfer-core")
include(":extensions:data-plane-transfer:data-plane-transfer-sync")
include(":extensions:data-plane:data-plane-spi")
include(":extensions:data-plane:data-plane-framework")
include(":extensions:data-plane:data-plane-http")
include(":extensions:data-plane-selector")
include(":extensions:data-plane-selector:selector-spi")
include(":extensions:data-plane-selector:selector-api")
include(":extensions:data-plane-selector:selector-core")
include(":extensions:data-plane-selector:selector-store")
include(":extensions:data-plane-selector:selector-client")
include(":extensions:azure:data-plane:common")
include(":extensions:azure:data-plane:storage")
include(":extensions:data-plane:data-plane-api")
include(":extensions:data-plane:integration-tests")
include(":extensions:sql:common")
include(":extensions:sql:pool:apache-commons-pool")
include(":extensions:http-receiver")

// modules for launchers, i.e. runnable compositions of the app
include(":launchers:basic")
include(":launchers:junit")
include(":launchers:test")
include(":launchers:ids-connector")
include(":launchers:registration-service-app")
include(":launchers:data-loader-cli")
include(":launchers:dpf-selector")
include(":launchers:data-plane-server")

// modules for code samples
include(":samples:other:copy-file-to-s3bucket")
include(":samples:other:dataseed:dataseed-aws")
include(":samples:other:dataseed:dataseed-azure")
include(":samples:other:dataseed:dataseed-policy")
include(":samples:other:run-from-junit")
include(":samples:other:custom-runtime")


// extension points for a connector
include(":spi")
include(":spi:core-spi")
include(":spi:transfer-spi")
include(":spi:contract-spi")
include(":spi:catalog-spi")
include(":spi:web-spi")
include(":spi:transport-spi")

//include(":openapi")

// numbered samples for the onboarding experience
include(":samples:01-basic-connector")
include(":samples:02-health-endpoint")
include(":samples:03-configuration")

include(":samples:04.0-file-transfer:consumer")
include(":samples:04.0-file-transfer:provider")
include(":samples:04.0-file-transfer:api")
include(":samples:04.0-file-transfer:transfer-file")

include(":samples:04.1-file-transfer-listener:consumer")
include(":samples:04.1-file-transfer-listener:listener")
include(":samples:04.1-file-transfer-listener:transfer-file")
include(":samples:04.1-file-transfer-listener:api")

include(":samples:04.2-modify-transferprocess:consumer")
include(":samples:04.2-modify-transferprocess:watchdog")
include(":samples:04.2-modify-transferprocess:simulator")

include(":samples:04.3-open-telemetry:micrometer")
include(":samples:04.3-open-telemetry:consumer")

include(":samples:05-file-transfer-cloud:consumer")
include(":samples:05-file-transfer-cloud:provider")
include(":samples:05-file-transfer-cloud:api")
include(":samples:05-file-transfer-cloud:data-seeder")
include(":samples:05-file-transfer-cloud:transfer-file")

include(":system-tests:e2e-transfer-test:runner")
include(":system-tests:e2e-transfer-test:backend-service")
include(":system-tests:e2e-transfer-test:control-plane")
include(":system-tests:e2e-transfer-test:data-plane")
include(":system-tests:runtimes:file-transfer-provider")
include(":system-tests:runtimes:file-transfer-consumer")
include(":system-tests:tests")
