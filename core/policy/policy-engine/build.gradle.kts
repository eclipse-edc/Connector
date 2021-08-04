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
        create<MavenPublication>("core.policy-engine") {
            artifactId = "core.policy-engine"
            from(components["java"])
        }
    }
}
