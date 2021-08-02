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
        create<MavenPublication>("policy-model") {
            artifactId = "edc.policy-model"
            from(components["java"])
        }
    }
}