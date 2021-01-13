val infoModelVersion: String by project

plugins {
    `java-library`
}

dependencies {     
    api(project(":spi"))
}

