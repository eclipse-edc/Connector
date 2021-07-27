/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":core:spi"))


}
publishing {
    publications {
        create<MavenPublication>("transfer-store-memory") {
            artifactId = "edc.transfer-process-store.memory"
            from(components["java"])
        }
    }
}