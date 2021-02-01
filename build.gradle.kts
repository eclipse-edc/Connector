plugins {
    `java-library`
}

repositories {
    jcenter()
}

val jetBrainsAnnotationsVersion: String by project
val jacksonVersion: String by project

subprojects {

    repositories {
        jcenter()
        maven {
            url = uri("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
        }
    }

}

allprojects {
    pluginManager.withPlugin("java-library") {
        dependencies {
            api("org.jetbrains:annotations:${jetBrainsAnnotationsVersion}")
            api("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
            api("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")
            api("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
            api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")

            testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")

        }

    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}
