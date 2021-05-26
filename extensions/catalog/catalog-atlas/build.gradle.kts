/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    api(project(":core"))
    api(project(":extensions:schema"))

    testImplementation(project(":distributions:junit"))
}
