/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:web-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":extensions:common:api:management-api-configuration"))
    testImplementation(project(":data-protocols:ids"))

    implementation(libs.jakarta.rsApi)
    testImplementation(project(":core:common:junit"))
}

edcBuild {
    swagger {
        apiGroup.set("management-api")
    }
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
