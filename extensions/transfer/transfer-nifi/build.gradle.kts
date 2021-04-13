val okHttpVersion: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    testImplementation("com.azure:azure-storage-blob:12.6.0")
}


