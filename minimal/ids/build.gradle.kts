/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":core:spi"))
    api(project(":minimal:ids:ids-spi"))
    api(project(":minimal:ids:ids-core"))
    api(project(":minimal:ids:ids-api-catalog"))
    api(project(":minimal:ids:ids-api-transfer"))
    api(project(":minimal:ids:ids-policy-mock"))
}

publishing {
    publications {
        create<MavenPublication>("ids") {
            artifactId = "edc.ids"
            from(components["java"])
        }
    }
}