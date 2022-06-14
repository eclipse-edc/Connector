plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val jupiterVersion: String by project
val rsApi: String by project
val openTelemetryVersion: String by project

dependencies {
    implementation(project(":core"))

    implementation(project(":extensions:api:observability"))

    implementation(project(":extensions:filesystem:vault-fs"))
    implementation(project(":extensions:filesystem:configuration-fs"))

    implementation(project(":extensions:iam:iam-mock"))

    implementation(project(":extensions:http"))

    implementation(project(":extensions:api:auth-tokenbased"))
    implementation(project(":extensions:api:data-management"))

    implementation(project(":data-protocols:ids")) {
        exclude("org.eclipse.dataspaceconnector","ids-token-validation")
    }
//    implementation(project(":data-protocols:ids:ids-api-multipart-endpoint-v1"))

//    implementation(project(":extensions:data-plane-transfer:data-plane-transfer-client"))
//    implementation(project(":extensions:data-plane-transfer:data-plane-transfer-sync"))
    implementation(project(":extensions:data-plane:data-plane-api"))
    implementation(project(":extensions:data-plane-selector:selector-client"))
    implementation(project(":extensions:data-plane-selector:selector-core"))
    implementation(project(":extensions:data-plane-selector:selector-store"))
    implementation(project(":extensions:data-plane:data-plane-framework"))
    implementation(project(":extensions:data-plane:data-plane-http"))
    implementation(project(":extensions:data-plane:data-plane-cloud-http"))

    implementation("io.opentelemetry:opentelemetry-extension-annotations:${openTelemetryVersion}")

    implementation(project(":extensions:data-plane:data-plane-spi"))

    implementation(project(":extensions:mindsphere:mindsphere-http"))

    implementation(project(":samples:other:file-transfer-http-to-http:transfer-file"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}

application {
    mainClass.set("com.siemens.mindsphere.datalake.edc.http.CatenaBaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("provider.jar")
}
