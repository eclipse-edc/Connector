plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {

        create("DependencyRulesPlugin") {
            id = "org.eclipse.dataspaceconnector.dependency-rules"
            implementationClass = "org.eclipse.dataspaceconnector.gradle.DependencyRulesPlugin"
        }
    }
}