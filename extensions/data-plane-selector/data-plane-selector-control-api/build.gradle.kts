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
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))

    implementation(project(":core:common:lib:core-lib"))
    implementation(project(":core:common:lib:jsonld-lib"))
    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:common:junit"))

    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(libs.restAssured)

}

edcBuild {
    swagger {
        apiGroup("control-api")
    }
}



