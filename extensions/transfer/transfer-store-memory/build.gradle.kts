/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))


}
publishing {
    publications {
        create<MavenPublication>("transfer-store-memory") {
            artifactId = "edc.transfer-store-memory"
            from(components["java"])
        }
    }
}