/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

rootProject.name = "dataspaceconnector"

// modules for common/util code

include(":common:util")

// EDC core modules
include(":core:bootstrap")
include(":core:policy:policy-engine")
include(":core:policy:policy-model")
include(":core:protocol:web")
include(":core:schema")
include(":core:transfer")

// modules that provide implementations for data ingress/egress
include(":data-protocols:ids:ids-api-catalog")
include(":data-protocols:ids:ids-api-transfer")
include(":data-protocols:ids:ids-core")
include(":data-protocols:ids:ids-policy-mock")
include(":data-protocols:ids:ids-spi")

// modules for technology- or cloud-provider extensions
include(":extensions:aws:s3:provision")
include(":extensions:aws:s3:s3-schema")
include(":extensions:azure:blob:blob-schema")
include(":extensions:azure:blob:provision")
include(":extensions:azure:events")
include(":extensions:azure:transfer-process-store-cosmos")
include(":extensions:azure:vault")
include(":extensions:azure:blob:api")
include(":extensions:atlas")
include(":extensions:filesystem:configuration-fs")
include(":extensions:filesystem:vault-fs")
include(":extensions:in-memory:metadata-memory")
include(":extensions:in-memory:policy-registry-memory")
include(":extensions:in-memory:transfer-store-memory")
include(":extensions:iam:iam-mock")
include(":extensions:iam:oauth2")


// modules for launchers, i.e. runnable compositions of the app
include(":launchers:demo-e2e")
include(":launchers:did-resolver")

// modules for code samples
include(":samples:commandline:client")
include(":samples:commandline:client-runtime")
include(":samples:copy-file-to-s3bucket")
include(":samples:copy-with-nifi:transfer")
include(":samples:copy-with-nifi:processors")
include(":samples:dataseed:dataseed-atlas")
include(":samples:dataseed:dataseed-aws")
include(":samples:dataseed:dataseed-azure")
include(":samples:dataseed:dataseed-nifi")
include(":samples:dataseed:dataseed-policy")
include(":samples:public-rest-api")
include(":samples:run-from-junit")
include(":samples:streaming")

// all implementations during/regarding the GaiaX Hackathon should go here:
include(":samples:gaiax-hackathon-1:ion")

// extension points for a connector
include(":spi")
