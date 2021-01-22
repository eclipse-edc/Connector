val infoModelVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    api(project(":extensions:ids:ids-spi"))

    api("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")
    
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

}


