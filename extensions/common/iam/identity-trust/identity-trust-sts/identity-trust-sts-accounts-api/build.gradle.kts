/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
    `java-library`
    `maven-publish`
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(project(":spi:common:web-spi"))
    api(project(":spi:common:auth-spi"))
    api(project(":spi:common:identity-trust-sts-spi"))
    api(project(":extensions:common:auth:auth-tokenbased"))

    implementation(libs.jakarta.rsApi)

    testImplementation(libs.jersey.common)
    testImplementation(libs.jersey.server)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(testFixtures(project(":spi:common:identity-trust-sts-spi")))
    testImplementation(libs.restAssured)
}

edcBuild {
    swagger {
        apiGroup.set("sts-accounts-api")
    }
}
