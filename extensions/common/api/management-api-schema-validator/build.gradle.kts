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
    `java-library`
}

dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:common:edr-store-spi"))
    api(project(":spi:common:validator-spi"))
    api(project(":spi:control-plane:transfer-spi"))
    api(project(":spi:control-plane:catalog-spi"))

    implementation(project(":extensions:common:api:lib:management-api-lib"))

    implementation(libs.jsonschema)
}


