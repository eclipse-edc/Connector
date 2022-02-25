plugins {
    `java-library`
}

dependencies {
    api(project(":core"))
    api(project(":data-protocols:ids"))
    api(project(":extensions:filesystem:vault-fs"))
    api(project(":extensions:http"))
    api(project(":extensions:iam:iam-mock"))
    api(project(":extensions:api:control"))
    api(project(":extensions:in-memory:assetindex-memory"))
    api(project(":extensions:in-memory:transfer-store-memory"))
    api(project(":extensions:in-memory:negotiation-store-memory"))
    api(project(":extensions:in-memory:contractdefinition-store-memory"))

    api(project(":extensions:data-plane-transfer:data-plane-transfer-spi"))
    api(project(":extensions:data-plane-transfer:data-plane-transfer-core"))
    api(project(":extensions:data-plane-transfer:data-plane-transfer-sync"))
    api(project(":extensions:http-receiver"))
}