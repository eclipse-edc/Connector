plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":spi:common:identity-trust-spi"))
    api(project(":spi:common:oauth2-spi"))
    api(project(":spi:common:participant-context-config-spi"))
    api(project(":spi:common:jwt-spi"))
    implementation(project(":extensions:common:iam:identity-trust:identity-trust-sts:lib:identity-trust-sts-remote-lib"))

    testImplementation(project(":core:common:junit"))
}

