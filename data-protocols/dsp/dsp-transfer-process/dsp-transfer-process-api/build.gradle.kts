/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":spi:control-plane:transfer-spi"))
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":extensions:common:http"))
    api(project(":data-protocols:dsp:dsp-api-configuration"))

    implementation(project(":extensions:common:json-ld"))
    implementation(project(":data-protocols:dsp:dsp-transform"))
    //TODO waiting for PRs #2759 & #2760
//    implementation(project(":data-protocols:dsp:dsp-transfer-process:dsp-transfer-process-transformer"))
    implementation(project(":data-protocols:dsp:dsp-transfer-process:dsp-transfer-process-spi"))

    implementation(libs.jakarta.rsApi)

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":extensions:common:http:jersey-core")))

    testImplementation(libs.restAssured)
}
