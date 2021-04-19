plugins {
    `java-library`
}

val awsVersion: String by project

dependencies {
    api(project(":spi"))
    api(project(":extensions:transfer:transfer-types-aws"))

    implementation("software.amazon.awssdk:s3:${awsVersion}")
    implementation("software.amazon.awssdk:sts:${awsVersion}")
    implementation("software.amazon.awssdk:iam:${awsVersion}")

}




