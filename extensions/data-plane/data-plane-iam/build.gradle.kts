/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Contributors to the Eclipse Foundation - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
//    api(project(":spi:common:core-spi"))
//    api(project(":spi:control-plane:control-plane-api-client-spi"))
    api(project(":spi:common:jwt-spi"))
    api(project(":spi:common:jwt-signer-spi"))
    api(project(":spi:common:token-spi"))
    api(project(":spi:data-plane:data-plane-spi"))
//
//    implementation(project(":spi:common:token-spi"))
//    implementation(project(":core:common:lib:store-lib"))
    implementation(project(":core:common:token-core"))
//    implementation(project(":core:common:boot"))
//    implementation(project(":core:common:lib:util-lib"))
//    implementation(project(":core:data-plane:data-plane-util"))
//    implementation(project(":extensions:common:http"))
//    implementation(project(":core:common:lib:state-machine-lib"))
//
//    implementation(libs.opentelemetry.instrumentation.annotations)
//
//    testImplementation(project(":core:common:lib:query-lib"))
    testImplementation(project(":core:common:junit"))
//    testImplementation(libs.awaitility)
//    testImplementation(testFixtures(project(":spi:data-plane:data-plane-spi")))
}


