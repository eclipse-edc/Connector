/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */


plugins {
    `java-library`
}

dependencies {
}
publishing {
    publications {
        create<MavenPublication>("core.policy-model") {
            artifactId = "core.policy-model"
            from(components["java"])
        }
    }
}
