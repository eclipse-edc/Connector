/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))

    testImplementation(project(":extensions:in-memory:transfer-store-memory"))

}


publishing {
    publications {
        create<MavenPublication>("transfer") {
            artifactId = "edc.transfer"
            from(components["java"])
        }
    }
}