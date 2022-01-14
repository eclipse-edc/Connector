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
 *
 */

rootProject.name = "dataspaceconnector"

// modules for common/util code

include(":common:util")

// EDC core modules
include(":core")
include(":core:policy:policy-engine")
include(":core:policy:policy-evaluator")
include(":core:transfer")
include(":core:contract")
include(":core:base")

// modules that provide implementations for data ingress/egress
include(":data-protocols:ids:ids-api-multipart-endpoint-v1")
include(":data-protocols:ids:ids-api-multipart-dispatcher-v1")
include(":data-protocols:ids:ids-core")
include(":data-protocols:ids:ids-policy-mock")
include(":data-protocols:ids:ids-spi")
include(":data-protocols:ids:ids-transform-v1")

include(":extensions:ion:ion-core")
include(":extensions:ion:ion-client")

// modules for technology- or cloud-provider extensions
include(":extensions:aws")
include(":extensions:api:control")
include(":extensions:api:observability")
include(":extensions:aws:s3:provision")
include(":extensions:aws:s3:s3-schema")
include(":extensions:aws:s3:s3-data-operator")
include(":extensions:aws:aws-test")
include(":extensions:azure:blobstorage")
include(":extensions:azure:blobstorage:blob-schema")
include(":extensions:azure:blobstorage:provision")
include(":extensions:azure:blobstorage:api")
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
include(":extensions:iam:decentralized-identifier")
include(":extensions:iam:decentralized-identifier:identity-did-spi")
include(":extensions:iam:decentralized-identifier:identity-did-core")
include(":extensions:iam:decentralized-identifier:identity-did-service")
include(":extensions:iam:decentralized-identifier:identity-did-web")
include(":extensions:iam:decentralized-identifier:identity-did-crypto")
include(":extensions:iam:decentralized-identifier:registration-service")
include(":extensions:iam:decentralized-identifier:registration-service-api")
include(":extensions:iam:decentralized-identifier:identity-common-test")
include(":extensions:iam:decentralized-identifier:dummy-credentials-verifier")
include(":extensions:catalog:federated-catalog-cache")
include(":extensions:catalog:federated-catalog-spi")
include(":extensions:transfer-functions:transfer-functions-spi")
include(":extensions:transfer-functions:transfer-functions-core")
include(":extensions:dataloading:dataloading-spi")
include(":extensions:dataloading:dataloading-asset")
include(":extensions:dataloading:dataloading-contractdef")
include(":extensions:policy:ids-policy")
include(":extensions:jdk-logger-monitor")
include(":extensions:http")
include(":extensions:http:jersey")
include(":extensions:http:jetty")
include(":extensions:inline-data-transfer:inline-data-transfer-core")
include(":extensions:inline-data-transfer:inline-data-transfer-spi")

// modules for launchers, i.e. runnable compositions of the app
include(":launchers:basic")
include(":launchers:junit")
include(":launchers:test")
include(":launchers:ids-connector")
include(":launchers:registration-service-app")
include(":launchers:data-loader-cli")

// modules for code samples
include(":samples:other:commandline:consumer")
include(":samples:other:commandline:consumer-runtime")
include(":samples:other:copy-file-to-s3bucket")
include(":samples:other:dataseed:dataseed-aws")
include(":samples:other:dataseed:dataseed-azure")
include(":samples:other:dataseed:dataseed-policy")
include(":samples:other:run-from-junit")
include(":samples:other:streaming")
include(":samples:other:file-transfer-s3-to-s3")
include(":samples:other:custom-runtime")

// all implementations during/regarding the GaiaX Hackathon should go here:


// extension points for a connector
include(":spi")
include(":spi:core-spi")
include(":spi:transfer-spi")
include(":spi:contract-spi")
include(":spi:catalog-spi")

// numbered samples for the onboarding experience
include(":samples:01-basic-connector")
include(":samples:02-health-endpoint")
include(":samples:03-configuration")

include(":samples:04-file-transfer:consumer")
include(":samples:04-file-transfer:provider")
include(":samples:04-file-transfer:api")
include(":samples:04-file-transfer:transfer-file")

include(":samples:05-file-transfer-cloud:consumer")
include(":samples:05-file-transfer-cloud:provider")
include(":samples:05-file-transfer-cloud:api")
include(":samples:05-file-transfer-cloud:data-seeder")
include(":samples:05-file-transfer-cloud:transfer-file")

include(":samples:04.1-file-transfer-listener:consumer")
include(":samples:04.1-file-transfer-listener:listener")
