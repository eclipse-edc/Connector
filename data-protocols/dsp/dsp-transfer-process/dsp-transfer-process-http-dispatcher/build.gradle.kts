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

    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":extensions:common:http"))

    api(project(":data-protocols:dsp:dsp-http-spi"))
    api(project(":data-protocols:dsp:dsp-transfer-process:dsp-transfer-process-spi"))
    implementation(project(":data-protocols:dsp:dsp-transfer-process:dsp-transfer-process-transformer"))
    implementation(project(":extensions:common:json-ld"))

    implementation("jakarta.json:jakarta.json-api:2.1.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jakarta-jsonp:2.14.2")

    implementation(libs.jakarta.rsApi)
}