plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    api(gradleApi())
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
    testing {
        enabled = false
    }
}