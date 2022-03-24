plugins {
    `kotlin-dsl`
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api(gradleApi())
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.mockito:mockito-core:4.2.0")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("com.github.javafaker:javafaker:1.0.2")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}