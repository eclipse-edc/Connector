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
}

dependencies {
    api(project(":spi:common:json-ld-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:data-plane:data-plane-spi"))

    implementation(project(":core:common:transform-core")) // for the transformer registry impl
    implementation(project(":core:common:jersey-providers"))
    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:common:junit"))
    testImplementation(project(":extensions:common:json-ld"))
}
edcBuild {
    swagger {
        apiGroup.set("control-api")
    }
}


