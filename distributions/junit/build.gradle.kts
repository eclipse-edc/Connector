
val securityType: String by rootProject.extra
val iamType: String by rootProject.extra
val configFs: String by rootProject.extra

plugins {
    `java-library`
}

dependencies {
    api(project(":core"))

    api("org.junit.jupiter:junit-jupiter-api:5.5.2")
    api("org.junit.jupiter:junit-jupiter-engine:5.5.2")

}
