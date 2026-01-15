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
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(project(":spi:common:web-spi"))
    api(project(":spi:common:json-ld-spi"))
    api(project(":spi:data-plane:data-plane-spi"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))

    implementation(project(":core:common:lib:transform-lib"))
    implementation(project(":extensions:data-plane:data-plane-signaling:data-plane-signaling-transform"))
    implementation(libs.jakarta.rsApi)

    testImplementation(libs.restAssured)
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))

}
edcBuild {
    swagger {
        apiGroup.set("control-api")
    }
}


