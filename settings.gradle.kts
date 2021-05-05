/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

rootProject.name = "dagx"

include(":spi")
include(":core")

include(":distributions:demo")
include(":distributions:azure")
include(":distributions:junit")

include(":extensions:schema")

include(":extensions:protocol:web")
include(":extensions:control-http")

include(":extensions:metadata:metadata-memory")

include(":extensions:transfer:transfer-core")
include(":extensions:transfer:transfer-nifi")
include(":extensions:transfer:transfer-demo-aws")
include(":extensions:transfer:transfer-provision-azure")
include(":extensions:transfer:transfer-provision-aws")
include(":extensions:transfer:transfer-store-memory")

include(":extensions:configuration:configuration-fs")

include(":extensions:security:security-fs")
include(":extensions:security:security-azure")
include(":extensions:iam:oauth2")
include(":extensions:iam:iam-mock")

include(":extensions:policy:policy-registry-memory")

include(":extensions:ids")
include(":extensions:ids:ids-spi")
include(":extensions:ids:ids-core")
include(":extensions:ids:ids-api-catalog")
include(":extensions:ids:ids-api-transfer")
include(":extensions:ids:ids-policy-mock")

include(":extensions:catalog:catalog-atlas")
include(":extensions:catalog:catalog-dataseed")


include(":policy:policy-model")
include(":policy:policy-engine")

include(":extensions:demo:demo-nifi")

include(":integration:integration-core")

include(":runtime")
include(":client-runtime")
include(":client")
