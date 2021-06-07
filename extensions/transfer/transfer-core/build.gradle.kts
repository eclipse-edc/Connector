/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))

    testImplementation(project(":extensions:transfer:transfer-store-memory"))

}


