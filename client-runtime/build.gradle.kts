
val securityType: String by rootProject.extra
val iamType: String by rootProject.extra
val configFs: String by rootProject.extra

plugins {
    `java-library`
}

dependencies {
    api(project(":core"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")

    println("Using security type: ${securityType}")

    if (securityType != "default") {
        api(project(":extensions:security:security-${securityType}"))
    }

    if (iamType == "oauth2") {
        api(project(":extensions:iam:oauth2"))
    }

    if (configFs == "enabled") {
        api(project(":extensions:configuration:configuration-fs"))
    }

}

