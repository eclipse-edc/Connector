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
    api(project(":data-protocols:dsp:dsp-http-spi"))
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:web-spi"))
    api(project(":spi:common:json-ld-spi"))

    api(project(":spi:control-plane:control-plane-spi"))

    implementation(project(":extensions:common:http:lib:jersey-providers-lib"))
    implementation(project(":data-protocols:dsp:dsp-lib:dsp-catalog-lib:dsp-catalog-validation-lib"))
    implementation(project(":data-protocols:dsp:dsp-lib:dsp-catalog-lib:dsp-catalog-http-api-lib"))

    implementation(libs.jakarta.rsApi)

    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(project(":core:common:junit"))
    testImplementation(libs.restAssured)
    testImplementation(testFixtures(project(":data-protocols:dsp:dsp-lib:dsp-catalog-lib:dsp-catalog-http-api-lib")))

}

edcBuild {
    swagger {
        apiGroup.set("dsp-api")
    }
}
