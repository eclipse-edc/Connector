/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    api(project(":extensions:ids:ids-spi"))
    api(project(":extensions:ids:ids-core"))
    api(project(":extensions:ids:ids-api-catalog"))
    api(project(":extensions:ids:ids-api-transfer"))
}

publishing {
    publications {
        create<MavenPublication>("ids") {
            artifactId = "edc.ids"
            from(components["java"])
        }
    }
}