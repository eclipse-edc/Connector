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
 */

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))
    api(project(":spi:common:aggregate-service-spi"))
    implementation(project(":core:common:util"))
    implementation(project(":extensions:common:api:management-api-configuration"))
    implementation(project(":extensions:common:api:api-core")) //for the exception mapper
    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:data-plane-selector:data-plane-selector-core"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":core:common:junit"))
}

edcBuild {
    swagger {
        apiGroup.set("management-api")
    }
}



