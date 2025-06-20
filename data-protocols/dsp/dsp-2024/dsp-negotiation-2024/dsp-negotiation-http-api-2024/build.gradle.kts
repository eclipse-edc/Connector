/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

plugins {
    `java-library`
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(project(":data-protocols:dsp:dsp-spi"))
    api(project(":data-protocols:dsp:dsp-2024:dsp-spi-2024"))
    api(project(":data-protocols:dsp:dsp-http-spi"))
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":extensions:common:json-ld"))

    implementation(project(":data-protocols:dsp:dsp-lib:dsp-negotiation-lib:dsp-negotiation-validation-lib"))
    implementation(project(":data-protocols:dsp:dsp-lib:dsp-negotiation-lib:dsp-negotiation-http-api-lib"))
    implementation(project(":extensions:common:http:lib:jersey-providers-lib"))

    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(libs.restAssured)
    testImplementation(testFixtures(project(":data-protocols:dsp:dsp-lib:dsp-negotiation-lib:dsp-negotiation-http-api-lib")))

}

edcBuild {
    swagger {
        apiGroup.set("dsp-api")
    }
}
