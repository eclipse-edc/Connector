val infoModelVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))

    api("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")

}


