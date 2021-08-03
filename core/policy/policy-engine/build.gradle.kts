/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */


plugins {
    `java-library`
}

dependencies {
    api(project(":core:policy:policy-model"))
}

publishing {
    publications {
        create<MavenPublication>("policy-engine") {
            artifactId = "dataspaceconnector.policy-engine"
            from(components["java"])
        }
    }
}
