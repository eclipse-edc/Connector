val okHttpVersion: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
}


