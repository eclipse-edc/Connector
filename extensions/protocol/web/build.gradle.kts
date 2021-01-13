val infoModelVersion: String by project
val servletApi: String by project
val rsApi: String by project
val jettyVersion: String by project
val jerseyVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))

    implementation("javax.ws.rs:javax.ws.rs-api:${rsApi}");

    implementation("org.eclipse.jetty:jetty-webapp:${jettyVersion}") {
        exclude("jetty-xml")
    }
    implementation("org.glassfish.jersey.core:jersey-server:${jerseyVersion}")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet-core:${jerseyVersion}")
    implementation("org.glassfish.jersey.core:jersey-common:${jerseyVersion}")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:${jerseyVersion}")
    implementation("org.glassfish.jersey.inject:jersey-hk2:${jerseyVersion}")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet:${jerseyVersion}")

}


