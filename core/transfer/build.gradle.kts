/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":core:spi"))

    testImplementation(project(":minimal:transfer:transfer-store-memory"))

}


publishing {
    publications {
        create<MavenPublication>("transfer") {
            artifactId = "edc.transfer"
            from(components["java"])
        }
    }
}