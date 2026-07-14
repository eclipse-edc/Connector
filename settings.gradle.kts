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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - refactoring
 *       ZF Friedrichshafen AG - add dependency & reorder entries
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - refactoring
 *       Mercedes-Benz Tech Innovation GmbH - publish public api context into dedicated swagger hub page
 *       Cofinity-X - make DSP versions pluggable
 *
 */

rootProject.name = "connector"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

// EDC core modules --------------------------------------------------------------------------------
include(":core:common:boot")
include(":core:common:connector-core")
include(":core:common:edr-store-core")
include(":core:common:document-cache-core")
include(":core:common:schema-validation-core")
include(":core:common:junit")
include(":core:common:runtime-core")
include(":core:common:token-core")
include(":core:common:participant-context-core")
include(":core:common:participant-context-connector-core")
include(":core:common:participant-context-connector-classic-core")
include(":core:common:participant-context-config-core")
include(":core:common:cel-core")
include(":core:common:task-core")
include(":core:common:security-core")

include(":core:common:lib:core-lib")
include(":core:common:lib:jsonld-lib")

include(":core:catalog-crawler:catalog-crawler-core")

include(":core:control-plane:control-plane-catalog")
include(":core:control-plane:control-plane-contract")
include(":core:control-plane:control-plane-contract-manager")
include(":core:control-plane:control-plane-contract-task-executor")
include(":core:control-plane:control-plane-core")
include(":core:control-plane:control-plane-aggregate-services")
include(":core:control-plane:control-plane-transform")
include(":core:control-plane:control-plane-transfer")
include(":core:control-plane:control-plane-transfer-manager")
include(":core:control-plane:control-plane-transfer-task-executor")
include(":core:control-plane:lib:control-plane-lib")

include(":core:data-plane-selector:data-plane-selector-core")

include(":core:policy-monitor:policy-monitor-core")

// data plane signaling
include(":data-protocols:data-plane-signaling")
include(":data-protocols:data-plane-signaling:data-plane-signaling-core")
include(":data-protocols:data-plane-signaling:data-plane-signaling-oauth2")
include(":data-protocols:data-plane-signaling:data-plane-signaling-spi")

// modules that provide implementations for data ingress/egress ------------------------------------
include(":data-protocols:dsp:dsp-spi")
include(":data-protocols:dsp:dsp-http-spi")
include(":data-protocols:dsp:dsp-version:dsp-version-http-api")
include(":data-protocols:dsp:dsp-virtual:dsp-metadata-http-api-virtual")

// dsp core
include(":data-protocols:dsp:dsp-core")
include(":data-protocols:dsp:dsp-core:dsp-http-api-base-configuration")
include(":data-protocols:dsp:dsp-core:dsp-http-core")
include(":data-protocols:dsp:dsp-core:dsp-catalog-http-dispatcher")
include(":data-protocols:dsp:dsp-core:dsp-negotiation-http-dispatcher")
include(":data-protocols:dsp:dsp-core:dsp-transfer-process-http-dispatcher")

// dsp lib
include(":data-protocols:dsp:dsp-lib")

// dsp version 2025/1
include(":data-protocols:dsp:dsp-2025")
include(":data-protocols:dsp:dsp-2025:dsp-spi-2025")
include(":data-protocols:dsp:dsp-2025:dsp-http-api-configuration-2025")
include(":data-protocols:dsp:dsp-2025:dsp-catalog-2025")
include(":data-protocols:dsp:dsp-2025:dsp-catalog-2025:dsp-catalog-http-api-2025")
include(":data-protocols:dsp:dsp-2025:dsp-catalog-2025:dsp-catalog-transform-2025")
include(":data-protocols:dsp:dsp-2025:dsp-transfer-process-2025")
include(":data-protocols:dsp:dsp-2025:dsp-transfer-process-2025:dsp-transfer-process-http-api-2025")
include(":data-protocols:dsp:dsp-2025:dsp-transfer-process-2025:dsp-transfer-process-transform-2025")
include(":data-protocols:dsp:dsp-2025:dsp-negotiation-2025")
include(":data-protocols:dsp:dsp-2025:dsp-negotiation-2025:dsp-negotiation-http-api-2025")
include(":data-protocols:dsp:dsp-2025:dsp-negotiation-2025:dsp-negotiation-transform-2025")

// dsp version 2025/1 virtual variant
include(":data-protocols:dsp:dsp-virtual:dsp-http-virtual-spi")
include(":data-protocols:dsp:dsp-virtual:dsp-http-core-virtual")
include(":data-protocols:dsp:dsp-virtual:dsp-2025-virtual")
include(":data-protocols:dsp:dsp-virtual:dsp-2025-virtual:dsp-catalog-http-api-2025-virtual")
include(":data-protocols:dsp:dsp-virtual:dsp-2025-virtual:dsp-negotiation-http-api-2025-virtual")
include(":data-protocols:dsp:dsp-virtual:dsp-2025-virtual:dsp-transfer-process-http-api-2025-virtual")
include(":data-protocols:dsp:dsp-virtual:dsp-2025-virtual:dsp-http-api-configuration-2025-virtual")

// modules for technology- or cloud-provider extensions --------------------------------------------
include(":extensions:common:api:api-core")
include(":extensions:common:api:api-observability")
include(":extensions:common:api:version-api")
include(":extensions:common:api:document-cache-api")
include(":extensions:common:api:schema-validation-api")
include(":extensions:common:auth:auth-tokenbased")
include(":extensions:common:auth:auth-delegated")
include(":extensions:common:auth:auth-configuration")
include(":extensions:common:crypto:ldp-verifiable-credentials")
include(":extensions:common:crypto:jwt-verifiable-credentials")
include(":extensions:common:crypto:lib:jws2020-lib")

include(":extensions:common:configuration:configuration-filesystem")
include(":extensions:common:events:events-cloud-http")
include(":extensions:common:events:events-nats")
include(":extensions:common:http")
include(":extensions:common:console-monitor")
include(":extensions:common:otel-monitor")
include(":extensions:common:http:jersey-core")
include(":extensions:common:http:jersey-micrometer")
include(":extensions:common:http:jetty-core")
include(":extensions:common:http:jetty-micrometer")
include(":extensions:common:iam:decentralized-identity")
include(":extensions:common:iam:decentralized-identity:identity-did-core")
include(":extensions:common:iam:decentralized-identity:identity-did-web")
include(":extensions:common:iam:iam-mock")
include(":extensions:common:iam:oauth2:oauth2-client")
include(":extensions:common:iam:verifiable-credentials")
include(":extensions:common:iam:decentralized-claims")
include(":extensions:common:iam:decentralized-claims:decentralized-claims-transform")
include(":extensions:common:iam:decentralized-claims:decentralized-claims-service")
include(":extensions:common:iam:decentralized-claims:decentralized-claims-core")
include(":extensions:common:iam:decentralized-claims:decentralized-claims-sts")
include(":extensions:common:iam:decentralized-claims:decentralized-claims-sts:decentralized-claims-sts-remote-client")
include(":extensions:common:iam:decentralized-claims:decentralized-claims-sts:decentralized-claims-sts-registry")
include(":extensions:common:iam:decentralized-claims:decentralized-claims-sts:decentralized-claims-sts-remote-registrar")
include(":extensions:common:iam:decentralized-claims:decentralized-claims-sts:decentralized-claims-sts-signature-registrar")
include(":extensions:common:iam:decentralized-claims:decentralized-claims-issuers-configuration")
include(":extensions:common:iam:decentralized-claims:decentralized-claims-cel")
include(":extensions:common:iam:decentralized-claims:decentralized-claims-store-sql")

include(":extensions:common:json-ld")
include(":extensions:common:metrics:micrometer-core")
include(":extensions:common:sql:sql-core")
include(":extensions:common:sql:sql-lease-spi")
include(":extensions:common:sql:sql-lease")
include(":extensions:common:sql:sql-lease-core")
include(":extensions:common:sql:sql-bootstrapper")
include(":extensions:common:sql:sql-test-fixtures")
include(":extensions:common:sql:sql-pool:sql-pool-apache-commons")
include(":extensions:common:transaction")
include(":extensions:common:transaction:transaction-atomikos")
include(":extensions:common:transaction:transaction-local")
include(":extensions:common:vault:vault-hashicorp")
include(":extensions:common:store:sql:edr-index-sql")
include(":extensions:common:store:sql:document-cache-store-sql")
include(":extensions:common:store:sql:schema-validation-store-sql")
include(":extensions:common:store:sql:jti-validation-store-sql")
include(":extensions:common:store:sql:cel-store-sql")
include(":extensions:common:store:sql:task-store-sql")
include(":extensions:common:encryption:aes-encryption")
include(":extensions:federated-catalog:store:sql:federated-catalog-cache-sql")
include(":extensions:federated-catalog:store:sql:target-node-directory-sql")

include(":extensions:common:api:management-api-configuration")
include(":extensions:common:api:management-api-schema-validator")
include(":extensions:common:api:management-api-authorization")
include(":extensions:common:api:management-api-oauth2-authentication")
include(":extensions:federated-catalog:api:federated-catalog-api")

include(":extensions:control-plane:api:management-api")
include(":extensions:control-plane:api:management-api:asset-api")
include(":extensions:control-plane:api:management-api:secrets-api")
include(":extensions:control-plane:api:management-api:catalog-api")
include(":extensions:control-plane:api:management-api:contract-agreement-api")
include(":extensions:control-plane:api:management-api:contract-definition-api")
include(":extensions:control-plane:api:management-api:contract-negotiation-api")
include(":extensions:control-plane:api:management-api:data-plane-selector-api")
include(":extensions:control-plane:api:management-api:management-api-test-fixtures")
include(":extensions:control-plane:api:management-api:policy-definition-api")
include(":extensions:control-plane:api:management-api:transfer-process-api")
include(":extensions:control-plane:api:management-api:edr-cache-api")
include(":extensions:control-plane:api:management-api-v5")
include(":extensions:control-plane:api:management-api-v5:asset-api-v5")
include(":extensions:control-plane:api:management-api-v5:policy-definition-api-v5")
include(":extensions:control-plane:api:management-api-v5:contract-definition-api-v5")
include(":extensions:control-plane:api:management-api-v5:catalog-api-v5")
include(":extensions:control-plane:api:management-api-v5:contract-negotiation-api-v5")
include(":extensions:control-plane:api:management-api-v5:contract-agreement-api-v5")
include(":extensions:control-plane:api:management-api-v5:transfer-process-api-v5")
include(":extensions:control-plane:api:management-api-v5:participant-context-api-v5")
include(":extensions:control-plane:api:management-api-v5:participant-context-config-api-v5")
include(":extensions:control-plane:api:management-api-v5:cel-api-v5")
include(":extensions:control-plane:api:management-api-v5:dataspace-profile-context-api-v5")
include(":extensions:control-plane:api:management-api-v5:discovery-api-v5")
include(":extensions:control-plane:api:management-api-v5:dcp-scope-api-v5")

include(":extensions:control-plane:store:sql:asset-index-sql")
include(":extensions:control-plane:store:sql:contract-definition-store-sql")
include(":extensions:control-plane:store:sql:contract-negotiation-store-sql")
include(":extensions:control-plane:store:sql:control-plane-sql")
include(":extensions:control-plane:store:sql:data-plane-instance-store-sql")
include(":extensions:control-plane:store:sql:dataspace-profile-store-sql")
include(":extensions:control-plane:store:sql:policy-definition-store-sql")
include(":extensions:control-plane:store:sql:participantcontext-store-sql")
include(":extensions:control-plane:store:sql:participantcontext-config-store-sql")
include(":extensions:control-plane:store:sql:transfer-process-store-sql")
include(":extensions:control-plane:callback:callback-event-dispatcher")
include(":extensions:control-plane:callback:callback-static-endpoint")
include(":extensions:control-plane:edr:edr-store-receiver")
include(":extensions:control-plane:tasks:nats:publisher:negotiation-tasks-publisher-nats")
include(":extensions:control-plane:tasks:nats:publisher:transfer-tasks-publisher-nats")
include(":extensions:control-plane:tasks:nats:subscriber:negotiation-tasks-subscriber-nats")
include(":extensions:control-plane:tasks:nats:subscriber:transfer-tasks-subscriber-nats")


include(":extensions:policy-monitor:store:sql:policy-monitor-store-sql")

// extension points for a connector ----------------------------------------------------------------
include(":spi:core-spi")
include(":spi:control-plane-spi")
include(":spi:decentralized-claims-spi")
include(":spi:dataspace-protocol-spi")
include(":core:common:junit-base")


// modules for system tests ------------------------------------------------------------------------
include(":system-tests:bom-tests")
include(":system-tests:nats-events-tests")
include(":system-tests:e2e-transfer-test:control-plane")
include(":system-tests:e2e-transfer-test:runner")
include(":system-tests:e2e-transfer-test:signaling-data-plane")
include(":system-tests:management-api:management-api-test-runner")
include(":system-tests:management-api:management-api-test-runtime")
include(":system-tests:protocol-2025-test")
include(":system-tests:tck:dcp-tck-tests")
include(":system-tests:tck:dsp-tck-connector-under-test")
include(":system-tests:tck:dsp-tck-virtual-connector-under-test")
include(":system-tests:tck:dsp-tck-tests")
include(":system-tests:tck:dps-tck-tests")
include(":system-tests:tck:dps-tck-connector-under-test")
include(":system-tests:tck:tck-extension")
include(":system-tests:tck:tasks-tck-extension")
include(":system-tests:telemetry:telemetry-test-runner")
include(":system-tests:telemetry:telemetry-test-runtime")
include(":system-tests:version-api:version-api-test-runner")
include(":system-tests:version-api:version-api-test-runtime")
include(":system-tests:e2e-federatedcatalog-tests:component-tests")
include(":system-tests:e2e-federatedcatalog-tests:end2end-test:catalog-runtime")
include(":system-tests:e2e-federatedcatalog-tests:end2end-test:connector-runtime")
include(":system-tests:e2e-federatedcatalog-tests:end2end-test:e2e-junit-runner")

// BOM modules ----------------------------------------------------------------
include(":dist:bom:controlplane-base-bom")
include(":dist:bom:controlplane-dcp-bom")
include(":dist:bom:controlplane-feature-sql-bom")
include(":dist:bom:controlplane-feature-dcp-bom")
include(":dist:bom:controlplane-virtual-base-bom")
include(":dist:bom:controlplane-virtual-feature-nats-bom")
include(":dist:bom:controlplane-virtual-feature-sql-bom")
include(":dist:bom:controlplane-virtual-feature-dcp-bom")
include(":dist:bom:federatedcatalog-base-bom")
include(":dist:bom:federatedcatalog-dcp-bom")
include(":dist:bom:federatedcatalog-feature-sql-bom")
