/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import java.io.FileInputStream
import java.util.*

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("com.bmuschko.docker-remote-api") version "6.7.0"
}


dependencies {
    implementation(project(":runtime"))
    implementation(project(":extensions:protocol:web"))
    implementation(project(":extensions:control-http"))

    implementation(project(":extensions:metadata:metadata-memory"))
    implementation(project(":extensions:transfer:transfer-core"))
    implementation(project(":extensions:transfer:transfer-store-memory"))
    implementation(project(":extensions:transfer:transfer-provision-aws"))
    implementation(project(":extensions:transfer:transfer-demo-aws"))
    implementation(project(":extensions:ids"))
    implementation(project(":extensions:demo:demo-nifi"))
    implementation(project(":extensions:demo:demo-ui-api"))

    implementation(project(":extensions:security:security-fs"))
    implementation(project(":extensions:policy:policy-registry-memory"))
    implementation(project(":extensions:iam:iam-mock"))
    implementation(project(":extensions:ids:ids-policy-mock"))
//    implementation(project(":extensions:iam:oauth2"))
    implementation(project(":extensions:configuration:configuration-fs"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")

}

val filename = "github.properties"
var email = ""
var user = "microsoft"
var pwd = ""
var url = ""
var imageName = ""

// initializes variables
tasks.register("initializer") {
    val initializer by tasks
    val configFile = project.file(filename)
    if (!configFile.exists()) {
        println("WARNING: No $filename file was found, default will be used. Publishing won't be available!")
    } else {
        val fis = FileInputStream(configFile)
        val prop = Properties()
        prop.load(fis)
        email = prop.getProperty("email")
        user = prop.getProperty("user")
        pwd = prop.getProperty("password")
        url = prop.getProperty("url")
    }
    imageName = "$user/dagx-demo:latest"

    if (url != "") {
        imageName = "$url/$imageName"
    }

    println("Will use the following docker config:")
    println("  - URL: $url")
    println("  - User: $user")
    println("  - Email: $email")
    println("  - Image: $imageName")

}


// generate docker file
val createDockerfile by tasks.creating(Dockerfile::class) {
    dependsOn("initializer")
    from("openjdk:11-jre-slim")
    runCommand("mkdir /app")
    copyFile("./build/libs/dagx-demo.jar", "/app/dagx-demo.jar")

    copyFile("secrets/dagx-vault.properties", "/app/dagx-vault.properties")
    copyFile("secrets/dagx-test-keystore.jks", "/app/dagx-test-keystore.jks")

    environmentVariable("DAGX_VAULT", "/app/dagx-vault.properties")
    environmentVariable("DAGX_KEYSTORE", "/app/dagx-test-keystore.jks")
    environmentVariable("DAGX_KEYSTORE_PASSWORD", "test123")
    exposePort(8181)

    entryPoint("java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/dagx-demo.jar")
}

// build the image
val buildDemo by tasks.creating(DockerBuildImage::class) {
    dependsOn("shadowJar", createDockerfile)
    dockerFile.set(project.file("${buildDir}/docker/Dockerfile"))
    inputDir.set(project.file("."))
    images.add(imageName)
}

// create demo container
val createDemoContainer by tasks.creating(DockerCreateContainer::class) {
    dependsOn(buildDemo)
    targetImageId(buildDemo.imageId)
    hostConfig.portBindings.set(listOf("8181:8181"))
    hostConfig.autoRemove.set(true)
    containerName.set("dagx-demo")
}

// start runtime demo in docker
val startDemo by tasks.creating(DockerStartContainer::class) {
    dependsOn(createDemoContainer)
    targetContainerId(createDemoContainer.containerId)
}

//publish to github
val publishDemo by tasks.creating(DockerPushImage::class) {
    dependsOn(buildDemo)

    registryCredentials.email.set(email)
    registryCredentials.username.set(user)
    registryCredentials.password.set(pwd)
    registryCredentials.url.set(imageName)
    images.add(imageName)
}

application {
    @Suppress("DEPRECATION")
    mainClassName = "com.microsoft.dagx.runtime.DagxRuntime"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dagx-demo.jar")
}
