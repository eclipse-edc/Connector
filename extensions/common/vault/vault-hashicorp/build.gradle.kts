/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */
plugins {
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:http-spi"))

    implementation(project(":core:common:util"))

    testImplementation(project(":core:common:junit"))
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.vault)
    implementation(libs.bouncyCastle.bcpkixJdk18on)

}


