/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

rootProject.name = "dagx"

// modules for common/util code
include(":common:azure")
include(":common:util")

// EDC Core modules
include(":edc-core:core")
include(":edc-core:iam:iam-mock")
include(":edc-core:iam:oauth2")
include(":edc-core:policy:policy-engine")
include(":edc-core:policy:policy-model")
include(":edc-core:protocol:web")
include(":edc-core:schema")
include(":edc-core:spi")
include(":edc-core:transfer")

// modules for a minimal installation
include(":minimal:configuration:configuration-fs")
include(":minimal:control-http")
include(":minimal:ids:ids-api-catalog")
include(":minimal:ids:ids-api-transfer")
include(":minimal:ids:ids-core")
include(":minimal:ids:ids-policy-mock")
include(":minimal:ids:ids-spi")
include(":minimal:metadata:metadata-memory")
include(":minimal:policy:policy-registry-memory")
include(":minimal:runtime")
include(":minimal:security:security-fs")
include(":minimal:transfer:transfer-store-memory")

// modules for cloud-provider extensions
include(":extensions:aws:s3:provision")
include(":extensions:aws:s3:s3-schema")
include(":extensions:azure:blob:blob-schema")
include(":extensions:azure:blob:provision")
include(":extensions:azure:events")
include(":extensions:azure:transfer-process-store-cosmos")
include(":extensions:azure:vault")
include(":extensions:catalog-atlas")
include(":extensions:demo:demo-nifi")

// modules for external components, such as NiFi processors
include(":external:nifi:processors")

// modules for code samples
include(":samples:commandline:client")
include(":samples:commandline:client-runtime")
include(":samples:copy-file-to-s3bucket")
include(":samples:copy-with-nifi")
include(":samples:dataseed:dataseed-atlas")
include(":samples:dataseed:dataseed-aws")
include(":samples:dataseed:dataseed-azure")
include(":samples:dataseed:dataseed-nifi")
include(":samples:dataseed:dataseed-policy")
include(":samples:public-rest-api")
include(":samples:run-from-junit")
include(":samples:streaming")
include(":distributions:demo-e2e")