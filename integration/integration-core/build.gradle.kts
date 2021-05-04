plugins {
    `java-library`
}

dependencies {
    api(project(":core"))
    api(project(":extensions:transfer:transfer-core"))
    api(project(":extensions:transfer:transfer-store-memory"))

    api(project(":extensions:transfer:transfer-provision-aws"))

    api(project(":extensions:ids:ids-core"))
//    testImplementation(project(":extensions:iam:oauth2"))
//    testImplementation(project(":extensions:iam:iam-mock"))

    testImplementation(project(":extensions:security:security-fs"))
    testImplementation(project(":distributions:junit"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")


}
