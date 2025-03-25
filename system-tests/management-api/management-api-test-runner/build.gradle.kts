/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    java
}

dependencies {
    testImplementation(project(":core:common:junit"))
    // gives access to the Json LD models, etc.
    testImplementation(project(":spi:common:json-ld-spi"))
    testImplementation(project(":data-protocols:dsp:dsp-spi"))
    testImplementation(project(":spi:control-plane:asset-spi"))
    testImplementation(project(":spi:control-plane:contract-spi"))
    testImplementation(project(":spi:data-plane-selector:data-plane-selector-spi"))
    testImplementation(project(":core:common:connector-core"))
    testImplementation(project(":core:common:edr-store-core"))

    //useful for generic DTOs etc.
    testImplementation(project(":spi:control-plane:policy-spi"))
    testImplementation(project(":spi:control-plane:transfer-spi"))

    //we need the JacksonJsonLd util class
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":extensions:common:json-ld"))

    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    testImplementation(project(":extensions:common:transaction:transaction-local"))
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)

    testImplementation(project(":extensions:control-plane:api:management-api:contract-definition-api"))
    testImplementation(project(":extensions:control-plane:api:management-api:contract-negotiation-api"))
    testImplementation(project(":extensions:control-plane:api:management-api:policy-definition-api"))
    testImplementation(project(":extensions:control-plane:api:management-api:transfer-process-api"))
    testImplementation(project(":extensions:control-plane:api:management-api:secrets-api"))
    testImplementation(project(":extensions:control-plane:api:management-api:edr-cache-api"))
    testImplementation(project(":extensions:data-plane-selector:data-plane-selector-api"))
}

edcBuild {
    publish.set(false)
}
