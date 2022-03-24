plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("DataspaceConnectorPlugin") {
            id = "org.eclipse.dataspaceconnector"
            implementationClass = "org.eclipse.dataspaceconnector.gradle.DataspaceConnectorPlugin"
        }
    }
}