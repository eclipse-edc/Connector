/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *       Cofinity-X - make DSP versions pluggable
 *
 */

plugins {
    `java-library`
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(project(":spi:core-spi"))
    api(project(":spi:control-plane-spi"))
    api(project(":data-protocols:dsp:dsp-spi"))
    api(project(":data-protocols:dsp:dsp-2025:dsp-spi-2025"))
    api(project(":data-protocols:dsp:dsp-http-spi"))

    implementation(project(":spi:core-spi"))
    implementation(project(":data-protocols:dsp:dsp-lib"))
    implementation(project(":core:common:lib:core-lib"))

    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))
    testImplementation(testFixtures(project(":data-protocols:dsp:dsp-lib")))

    testImplementation(libs.restAssured)
}

edcBuild {
    swagger {
        apiGroup("dsp-api")
    }
}
