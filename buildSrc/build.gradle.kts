plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {

        create("DependencyRulesPlugin") {
            id = "org.eclipse.edc.dependency-rules"
            implementationClass = "org.eclipse.edc.gradle.DependencyRulesPlugin"
        }
    }
}