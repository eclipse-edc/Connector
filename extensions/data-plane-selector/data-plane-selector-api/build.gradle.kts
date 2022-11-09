/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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
 */plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":extensions:common:http"))
    api(project(":extensions:common:configuration:configuration-filesystem"))
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))
    implementation(project(":core:common:util"))
    implementation(libs.jakarta.rsApi)
    implementation(project(":extensions:common:api:api-core")) //for the exception mapper


    testImplementation(project(":core:data-plane-selector:data-plane-selector-core"))

    testImplementation(libs.okhttp)
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":core:common:junit"))
}


publishing {
    publications {
        create<MavenPublication>("data-plane-selector-api") {
            artifactId = "data-plane-selector-api"
            from(components["java"])
        }
    }
}
