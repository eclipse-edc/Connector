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
    id("io.swagger.core.v3.swagger-gradle-plugin")
}


dependencies {
    api(project(":spi:control-plane:transfer-spi"))
    api(project(":spi:control-plane:control-plane-spi"))
    api(project(":extensions:common:http"))

    api(project(":data-protocols:dsp:dsp-api-configuration"))

    implementation("com.apicatalog:titanium-json-ld:1.3.1")
    implementation("jakarta.json:jakarta.json-api:2.1.1")
    implementation("org.eclipse.parsson:parsson:1.1.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jakarta-jsonp:2.14.2")

    implementation(root.jakarta.rsApi)

    testImplementation(project(":extensions:common:http"))
    testImplementation(project(":core:common:junit"))
    testImplementation(project(":core:control-plane:control-plane-core"))
    testImplementation(project(":extensions:common:iam:iam-mock"))
    implementation(project(":extensions:common:json-ld"))
    implementation(project(":data-protocols:dsp:dsp-transform"))

    testImplementation(root.restAssured)
}
