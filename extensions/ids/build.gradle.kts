plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    api(project(":extensions:ids:ids-spi"))
    api(project(":extensions:ids:ids-core"))
    api(project(":extensions:ids:ids-api-catalog"))
    api(project(":extensions:ids:ids-api-transfer"))
}


