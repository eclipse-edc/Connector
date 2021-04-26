
plugins {
    `java-library`
}

dependencies {
    api(project(":core"))
    api(project(":extensions:transfer:transfer-core"))
    api(project(":extensions:transfer:transfer-store-memory"))

    api(project(":extensions:transfer:transfer-provision-aws"))
    
    testImplementation(project(":extensions:security:security-fs"))
    testImplementation(project(":distributions:junit"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")


}
