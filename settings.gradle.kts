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
include(":core:bootstrap")
include(":core:policy:policy-engine")
include(":core:policy:policy-model")
include(":core:protocol:web")
include(":core:schema")
include(":core:transfer")
include(":core:contract")

// modules that provide implementations for data ingress/egress
include(":data-protocols:ids:ids-api-catalog")
include(":data-protocols:ids:ids-api-transfer")
include(":data-protocols:ids:ids-core")
include(":data-protocols:ids:ids-policy-mock")
include(":data-protocols:ids:ids-spi")
include(":data-protocols:ion:ion-core")
include(":data-protocols:ion:ion-client")

// modules for technology- or cloud-provider extensions
include(":extensions:aws")
include(":extensions:api:control-rest")
include(":extensions:aws:s3:provision")
include(":extensions:aws:s3:s3-schema")
include(":extensions:aws:aws-test")
include(":extensions:azure:blob")
include(":extensions:azure:blob:blob-schema")
include(":extensions:azure:blob:provision")
include(":extensions:azure:events")
include(":extensions:azure:events-config")
include(":extensions:azure:azure-test")
include(":extensions:azure:transfer-process-store-cosmos")
include(":extensions:azure:vault")
include(":extensions:azure:blob:api")
include(":extensions:filesystem:configuration-fs")
include(":extensions:filesystem:vault-fs")
include(":extensions:in-memory:metadata-memory")
include(":extensions:in-memory:policy-registry-memory")
include(":extensions:in-memory:transfer-store-memory")
include(":extensions:in-memory:did-document-store-inmem")
include(":extensions:in-memory:identity-hub-memory")
include(":extensions:iam:iam-mock")
include(":extensions:iam:oauth2")
include(":extensions:iam:distributed-identity")
include(":extensions:iam:distributed-identity:identity-did-spi")
include(":extensions:iam:distributed-identity:identity-did-core")
include(":extensions:iam:distributed-identity:identity-did-service")
include(":extensions:iam:distributed-identity:registration-service")
include(":extensions:iam:distributed-identity:registration-service-api")
include(":extensions:iam:distributed-identity:identity-common-test")
include(":extensions:catalog:catalog-service")


// modules for launchers, i.e. runnable compositions of the app
include(":launchers:basic")
include(":launchers:demo-e2e")
include(":launchers:junit")
include(":launchers:registration-service-app")
include(":launchers:simple-provider")
include(":launchers:simple-consumer")

// modules for code samples
include(":samples:other:commandline:consumer")
include(":samples:other:commandline:consumer-runtime")
include(":samples:other:copy-file-to-s3bucket")
include(":samples:other:copy-between-azure-and-s3")
include(":samples:other:dataseed:dataseed-aws")
include(":samples:other:dataseed:dataseed-azure")
include(":samples:other:dataseed:dataseed-policy")
include(":samples:other:public-rest-api")
include(":samples:other:run-from-junit")
include(":samples:other:streaming")
include(":samples:demo-asset-index")
include(":samples:demo-contract-framework")
include(":samples:demo-asset-index")
include(":samples:demo-contract-framework")
include(":samples:identity-gaiax-verifier")
include(":samples:other:identity-gaiax-verifier")
include(":samples:other:file-transfer-s3-to-s3")
include(":samples:other:custom-runtime")

// all implementations during/regarding the GaiaX Hackathon should go here:


// all implementations during/regarding the GaiaX Hackathon should go here:
include(":samples:gaiax-hackathon-1:identity:cloud-transfer")

include(":samples:gaiax-hackathon-1:identity:control-rest")
include(":samples:gaiax-hackathon-1:identity:catalog-service")

include(":samples:identity-gaiax-verifier")

include(":samples:gaiax-hackathon-1:identity:launchers:registration-service-app")
include(":samples:gaiax-hackathon-1:identity:launchers:simple-provider")
include(":samples:gaiax-hackathon-1:identity:launchers:simple-consumer")

// extension points for a connector
include(":spi")

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
include(":samples:05-file-transfer-cloud:transfer-file")
