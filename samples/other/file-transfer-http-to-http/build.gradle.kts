val awsVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

dependencies {
    implementation(project(":core"))

    implementation(project(":data-protocols:ids"))

    implementation(project(":extensions:api:control"))
    implementation(project(":extensions:api:data-management"))
    implementation(project(":extensions:api:observability"))

    implementation(project(":extensions:filesystem:configuration-fs"))

    implementation(project(":extensions:iam:iam-mock"))

    implementation(project(":extensions:in-memory:assetindex-memory"))
    implementation(project(":extensions:in-memory:transfer-store-memory"))
    implementation(project(":extensions:in-memory:contractdefinition-store-memory"))
    implementation(project(":extensions:in-memory:policy-store-memory"))
    implementation(project(":extensions:in-memory:transfer-store-memory"))
    implementation(project(":extensions:in-memory:negotiation-store-memory"))

    implementation(project(":extensions:http"))

    implementation(project(":extensions:mindsphere:mindsphere-http"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    implementation(project(":samples:other:file-transfer-http-to-http:api"))
}

application {
    mainClass.set("com.siemens.mindsphere.datalake.edc.http.CatenaBaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("file-transfer-http-to-http.jar")
}
