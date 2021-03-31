plugins {
    `java-library`
}

val s3Version: String by project

dependencies {
    api(project(":spi"))

    implementation("software.amazon.awssdk:s3:${s3Version}")

}




