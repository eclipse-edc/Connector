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
    implementation(project(":spi:common:core-spi"))
    implementation(project(":spi:control-plane:contract-spi"))
    implementation(project(":spi:control-plane:asset-spi"))
    implementation(project(":spi:control-plane:control-plane-spi"))
    implementation(project(":spi:common:web-spi"))
    implementation(libs.jakarta.rsApi)
}

// If the EDC Build Plugin is used, every module gets visited during Publishing by default.
// Single modules can be excluded by setting the "publish" flag to false:

edcBuild {
    publish.set(false)
}