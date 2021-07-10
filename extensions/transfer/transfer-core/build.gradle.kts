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


publishing {
    publications {
        create<MavenPublication>("transfer-core") {
            artifactId = "edc.transfer-core"
            from(components["java"])
        }
    }
}