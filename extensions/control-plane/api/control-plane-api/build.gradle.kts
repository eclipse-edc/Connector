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

    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":spi:control-plane:control-plane-api-client-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:common:auth-spi"))
    implementation(project(":extensions:common:api:control-api-configuration"))

    implementation(root.jakarta.rsApi)
    implementation(root.jakarta.validation)
    implementation(root.jersey.beanvalidation) //for validation

    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":extensions:common:auth:auth-tokenbased"))
    testImplementation(root.restAssured)
    testImplementation(root.awaitility)
}

edcBuild {
    swagger {
        apiGroup.set("control-api")
    }
}



