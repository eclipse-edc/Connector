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

    implementation(project(":core:common:lib:transform-lib"))
    implementation(project(":core:control-plane:control-plane-transform"))

    testImplementation(project(":spi:common:edr-store-spi"))
    testImplementation(project(":core:common:connector-core"))
    testImplementation(project(":core:control-plane:control-plane-transform"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:common:lib:json-ld-lib"))
    testImplementation(project(":extensions:common:json-ld"))
    testImplementation(project(":extensions:common:http:jetty-core"))
    testImplementation(project(":extensions:common:http:jersey-core"))
    testImplementation(project(":extensions:common:api:management-api-configuration"))
    testImplementation(project(":extensions:control-plane:api:management-api:contract-definition-api"))
    testImplementation(project(":extensions:control-plane:api:management-api:contract-negotiation-api"))
    testImplementation(project(":extensions:control-plane:api:management-api:policy-definition-api"))
    testImplementation(project(":extensions:control-plane:api:management-api:transfer-process-api"))
    testImplementation(project(":extensions:control-plane:api:management-api:secrets-api"))
    testImplementation(project(":extensions:control-plane:api:management-api:edr-cache-api"))
}

edcBuild {
    swagger {
        apiGroup.set("management-api")
    }
}


