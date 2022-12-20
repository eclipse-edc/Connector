/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *       ZF Friedrichshafen AG - add dependency & reorder entries
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

rootProject.name = "connector"

// this is needed to have access to snapshot builds of plugins
pluginManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            from("org.eclipse.edc:edc-versions:0.0.1-20221219-SNAPSHOT")
            // this is not part of the published EDC Version Catalog, so we'll just "amend" it
            library("dnsOverHttps", "com.squareup.okhttp3", "okhttp-dnsoverhttps").versionRef("okhttp")
        }
    }
}

// EDC core modules --------------------------------------------------------------------------------
include(":core:common:connector-core")
include(":core:common:boot")
include(":core:common:jwt-core")
include(":core:common:policy-engine")
include(":core:common:policy-evaluator")
include(":core:common:state-machine")
include(":core:common:util")
include(":core:common:junit")


include(":core:control-plane:contract-core")
include(":core:control-plane:control-plane-core")
include(":core:control-plane:control-plane-aggregate-services")
include(":core:control-plane:transfer-core")
include(":core:control-plane:control-plane-api")
include(":core:control-plane:control-plane-api-client")


include(":core:data-plane:data-plane-util")
include(":core:data-plane:data-plane-core")
include(":core:data-plane:data-plane-framework")

include(":core:data-plane-selector:data-plane-selector-core")

// modules that provide implementations for data ingress/egress ------------------------------------
include(":data-protocols:ids:ids-api-configuration")
include(":data-protocols:ids:ids-api-multipart-endpoint-v1")
include(":data-protocols:ids:ids-api-multipart-dispatcher-v1")
include(":data-protocols:ids:ids-core")
include(":data-protocols:ids:ids-jsonld-serdes")
include(":data-protocols:ids:ids-spi")
include(":data-protocols:ids:ids-token-validation")
include(":data-protocols:ids:ids-transform-v1")

// modules for technology- or cloud-provider extensions --------------------------------------------
include(":extensions:common:api:api-core")
include(":extensions:common:api:api-observability")
include(":extensions:common:auth:auth-basic")
include(":extensions:common:auth:auth-tokenbased")
include(":extensions:common:aws:aws-s3-test")
include(":extensions:common:aws:aws-s3-core")
include(":extensions:common:azure:azure-eventgrid")
include(":extensions:common:azure:azure-resource-manager")
include(":extensions:common:azure:azure-test")
include(":extensions:common:azure:azure-blob-core")
include(":extensions:common:azure:azure-cosmos-core")
include(":extensions:common:configuration:configuration-filesystem")
include(":extensions:common:events:events-cloud-http")
include(":extensions:common:gcp:gcp-core")
include(":extensions:common:http")
include(":extensions:common:http:jersey-core")
include(":extensions:common:http:jersey-micrometer")
include(":extensions:common:http:jetty-core")
include(":extensions:common:http:jetty-micrometer")
include(":extensions:common:iam:decentralized-identity")
include(":extensions:common:iam:decentralized-identity:identity-did-test")
include(":extensions:common:iam:decentralized-identity:identity-did-core")
include(":extensions:common:iam:decentralized-identity:identity-did-crypto")
include(":extensions:common:iam:decentralized-identity:identity-did-service")
include(":extensions:common:iam:decentralized-identity:identity-did-web")
include(":extensions:common:iam:iam-mock")
include(":extensions:common:iam:oauth2:oauth2-daps")
include(":extensions:common:iam:oauth2:oauth2-core")
include(":extensions:common:metrics:micrometer-core")
include(":extensions:common:monitor:monitor-jdk-logger")
include(":extensions:common:sql:sql-core")
include(":extensions:common:sql:sql-lease")
include(":extensions:common:sql:sql-pool:sql-pool-apache-commons")
include(":extensions:common:transaction")
include(":extensions:common:transaction:transaction-atomikos")
include(":extensions:common:transaction:transaction-local")
include(":extensions:common:vault:vault-azure")
include(":extensions:common:vault:vault-filesystem")
include(":extensions:common:vault:vault-hashicorp")

include(":extensions:common:api:control-api-configuration")
include(":extensions:common:api:management-api-configuration")

include(":extensions:control-plane:api:management-api")
include(":extensions:control-plane:api:management-api:asset-api")
include(":extensions:control-plane:api:management-api:catalog-api")
include(":extensions:control-plane:api:management-api:contract-agreement-api")
include(":extensions:control-plane:api:management-api:contract-definition-api")
include(":extensions:control-plane:api:management-api:contract-negotiation-api")
include(":extensions:control-plane:api:management-api:policy-definition-api")
include(":extensions:control-plane:api:management-api:transfer-process-api")
include(":extensions:control-plane:transfer:transfer-data-plane")
include(":extensions:control-plane:transfer:transfer-pull-http-receiver")
include(":extensions:control-plane:provision:provision-blob")
include(":extensions:control-plane:provision:provision-gcs")
include(":extensions:control-plane:provision:provision-http")
include(":extensions:control-plane:provision:provision-aws-s3")
include(":extensions:control-plane:provision:provision-oauth2")
include(":extensions:control-plane:store:cosmos:asset-index-cosmos")
include(":extensions:control-plane:store:cosmos:contract-definition-store-cosmos")
include(":extensions:control-plane:store:cosmos:contract-negotiation-store-cosmos")
include(":extensions:control-plane:store:cosmos:control-plane-cosmos")
include(":extensions:control-plane:store:cosmos:policy-definition-store-cosmos")
include(":extensions:control-plane:store:cosmos:transfer-process-store-cosmos")
include(":extensions:control-plane:store:sql:asset-index-sql")
include(":extensions:control-plane:store:sql:contract-definition-store-sql")
include(":extensions:control-plane:store:sql:contract-negotiation-store-sql")
include(":extensions:control-plane:store:sql:control-plane-sql")
include(":extensions:control-plane:store:sql:policy-definition-store-sql")
include(":extensions:control-plane:store:sql:transfer-process-store-sql")

include(":extensions:data-plane:data-plane-api")
include(":extensions:data-plane:data-plane-client")
include(":extensions:data-plane:data-plane-azure-storage")
include(":extensions:data-plane:data-plane-azure-data-factory")
include(":extensions:data-plane:data-plane-http")
include(":extensions:data-plane:data-plane-aws-s3")
include(":extensions:data-plane:data-plane-google-storage")
include(":extensions:data-plane:data-plane-integration-tests")
include(":extensions:data-plane:store:sql:data-plane-store-sql")
include(":extensions:data-plane:store:cosmos:data-plane-store-cosmos")



include(":extensions:data-plane-selector:data-plane-selector-api")
include(":extensions:data-plane-selector:data-plane-selector-client")
include(":extensions:data-plane-selector:store:sql:data-plane-instance-store-sql")
include(":extensions:data-plane-selector:store:cosmos:data-plane-instance-store-cosmos")

// modules for launchers, i.e. runnable compositions of the app ------------------------------------
include(":launchers:data-plane-server")
include(":launchers:dpf-selector")
include(":launchers:ids-connector")

// numbered samples for the onboarding experience --------------------------------------------------
include(":samples:01-basic-connector")
include(":samples:02-health-endpoint")
include(":samples:03-configuration")

include(":samples:04.0-file-transfer:consumer")
include(":samples:04.0-file-transfer:provider")
include(":samples:04.0-file-transfer:integration-tests")
include(":samples:04.0-file-transfer:transfer-file")
include(":samples:04.0-file-transfer:status-checker")


include(":samples:04.1-file-transfer-listener:consumer")
include(":samples:04.1-file-transfer-listener:file-transfer-listener-integration-tests")
include(":samples:04.1-file-transfer-listener:listener")

include(":samples:04.2-modify-transferprocess:api")
include(":samples:04.2-modify-transferprocess:consumer")
include(":samples:04.2-modify-transferprocess:modify-transferprocess-sample-integration-tests")
include(":samples:04.2-modify-transferprocess:simulator")
include(":samples:04.2-modify-transferprocess:watchdog")

include(":samples:04.3-open-telemetry:consumer")
include(":samples:04.3-open-telemetry:provider")

include(":samples:05-file-transfer-cloud:consumer")
include(":samples:05-file-transfer-cloud:provider")
include(":samples:05-file-transfer-cloud:transfer-file")

// modules for code samples ------------------------------------------------------------------------
include(":samples:other:custom-runtime")

// extension points for a connector ----------------------------------------------------------------
include(":spi:common:auth-spi")
include(":spi:common:catalog-spi")
include(":spi:common:core-spi")
include(":spi:common:identity-did-spi")
include(":spi:common:http-spi")
include(":spi:common:jwt-spi")
include(":spi:common:oauth2-spi")
include(":spi:common:policy-engine-spi")
include(":spi:common:policy-model")
include(":spi:common:aggregate-service-spi")
include(":spi:common:transaction-datasource-spi")
include(":spi:common:transaction-spi")
include(":spi:common:transform-spi")
include(":spi:common:web-spi")

include(":spi:control-plane:contract-spi")
include(":spi:control-plane:control-plane-spi")
include(":spi:control-plane:transfer-data-plane-spi")
include(":spi:control-plane:policy-spi")
include(":spi:control-plane:transfer-spi")
include(":spi:control-plane:control-plane-api-client-spi")


include(":spi:data-plane:data-plane-spi")

include(":spi:data-plane-selector:data-plane-selector-spi")

// modules for system tests ------------------------------------------------------------------------
include(":system-tests:azure-data-factory-tests")
include(":system-tests:azure-tests")
include(":system-tests:e2e-transfer-test:backend-service")
include(":system-tests:e2e-transfer-test:control-plane")
include(":system-tests:e2e-transfer-test:control-plane-cosmosdb")
include(":system-tests:e2e-transfer-test:control-plane-postgresql")
include(":system-tests:e2e-transfer-test:data-plane")
include(":system-tests:e2e-transfer-test:runner")
include(":system-tests:runtimes:azure-data-factory-transfer-consumer")
include(":system-tests:runtimes:azure-data-factory-transfer-provider")
include(":system-tests:runtimes:azure-storage-transfer-consumer")
include(":system-tests:runtimes:azure-storage-transfer-provider")
include(":system-tests:runtimes:file-transfer-consumer")
include(":system-tests:runtimes:file-transfer-provider")
include(":system-tests:tests")
