/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":edc:spi"))

    testImplementation(project(":minimal:transfer:transfer-store-memory"))

}


publishing {
    publications {
        create<MavenPublication>("transfer-core") {
            artifactId = "edc.transfer-core"
            from(components["java"])
        }
    }
}