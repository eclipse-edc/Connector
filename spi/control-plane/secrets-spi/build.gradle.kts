/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
}


dependencies {
    api(project(":spi:common:core-spi"))
    testImplementation(project(":core:common:lib:json-lib"))
}
