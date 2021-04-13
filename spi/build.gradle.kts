val jacksonVersion: String by project
val okHttpVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
    api("com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}")
    api("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")
    api("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    api(project(":policy:policy-model"))
}

