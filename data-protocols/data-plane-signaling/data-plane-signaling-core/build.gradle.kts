/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

plugins {
    `java-library`
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))
    api(project(":data-protocols:data-plane-signaling:data-plane-signaling-spi"))
    implementation(project(":core:common:lib:core-lib"))
    implementation(libs.jakarta.annotation)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(testFixtures(project(":core:common:lib:core-lib")))
    testImplementation(libs.restAssured)
    testImplementation(libs.wiremock)
    
}

edcBuild {
    swagger {
        apiGroup("signaling-api", "org.eclipse.edc.signaling.port.api.signaling")
        apiGroup("management-api", "org.eclipse.edc.signaling.port.api.management")
    }
}
