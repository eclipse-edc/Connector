plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:identity-trust-spi"))
    api(project(":spi:common:oauth2-spi"))
    api(project(":spi:common:jwt-spi"))
    implementation(project(":extensions:common:iam:identity-trust:identity-trust-sts:identity-trust-sts-remote"))

    testImplementation(project(":core:common:junit"))
}

