val infoModelVersion: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":extensions:ids:ids-spi"))

    api("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")

}


