/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    api(project(":data-protocols:ids:ids-spi"))
    api(project(":data-protocols:ids:ids-core"))
    api(project(":data-protocols:ids:ids-api-catalog"))
    api(project(":data-protocols:ids:ids-api-transfer"))
    api(project(":data-protocols:ids:ids-policy-mock"))
}

publishing {
    publications {
        create<MavenPublication>("ids") {
        artifactId = "edc.ids"
            from(components["java"])
        }
    }
}