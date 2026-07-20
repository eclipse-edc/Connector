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
    testImplementation(project(":spi:core-spi"))
    testImplementation(project(":data-protocols:dsp:dsp-spi"))
    testImplementation(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
    testImplementation(project(":spi:control-plane-spi"))
    testImplementation(project(":spi:decentralized-claims-spi"))
    testImplementation(project(":core:common:connector-core"))
    testImplementation(project(":core:control-plane:control-plane-transform"))
    testImplementation(project(":core:common:participant-context-config-core"))

    //useful for generic DTOs etc.

    //we need the JacksonJsonLd util class
    testImplementation(project(":core:common:lib:jsonld-lib"))
    testImplementation(project(":core:common:lib:core-lib"))
    testImplementation(project(":extensions:common:json-ld"))

    testImplementation(libs.restAssured)
    testImplementation(libs.awaitility)
    testImplementation(testFixtures(project(":extensions:common:sql:sql-test-fixtures")))
    testImplementation(project(":extensions:common:transaction:transaction-local"))
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)

    testImplementation(testFixtures(project(":core:common:lib:core-lib")))
}

edcBuild {
    publish.set(false)
}
