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
 *
 */

plugins {
    java
}

dependencies {
    implementation(project(":spi:common:core-spi"))
    implementation(project(":spi:control-plane:contract-spi"))
    implementation(project(":spi:control-plane:asset-spi"))
    implementation(project(":spi:control-plane:control-plane-spi"))
    implementation(project(":spi:common:web-spi"))
    implementation(project(":spi:data-plane:data-plane-spi"))
    implementation(libs.jakarta.rsApi)
    implementation(libs.nimbus.jwt)

}

