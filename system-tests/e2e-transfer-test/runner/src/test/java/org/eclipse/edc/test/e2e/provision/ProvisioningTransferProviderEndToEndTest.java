/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.provision;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import okhttp3.Request;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Deprovisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Provisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGenerator;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.Runtimes;
import org.eclipse.edc.test.e2e.TransferEndToEndParticipant;
import org.eclipse.edc.test.e2e.TransferEndToEndTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class ProvisioningTransferProviderEndToEndTest {


    private static final int SOURCE_BACKEND_PORT = getFreePort();

    private static class TestProviderProvisionerExtension implements ServiceExtension {

        @Inject
        private ResourceDefinitionGeneratorManager resourceDefinitionGeneratorManager;

        @Inject
        private ProvisionerManager provisionerManager;

        @Inject
        private EdcHttpClient httpClient;

        @Inject
        private DataPlaneManager dataPlaneManager;

        @Override
        public void initialize(ServiceExtensionContext context) {
            resourceDefinitionGeneratorManager.registerProviderGenerator(new AddHeaderResourceGenerator());
            resourceDefinitionGeneratorManager.registerProviderGenerator(new AsyncResourceGenerator());

            provisionerManager.register(new AddHeaderProvisioner());
            provisionerManager.register(new CallEndpointDeprovisioner(httpClient));
            provisionerManager.register(new AsyncProvisioner(dataPlaneManager));
            provisionerManager.register(new AsyncDeprovisioner());
        }

        private static class CallEndpointDeprovisioner implements Deprovisioner {

            private final EdcHttpClient httpClient;

            CallEndpointDeprovisioner(EdcHttpClient httpClient) {
                this.httpClient = httpClient;
            }

            @Override
            public String supportedType() {
                return "AddHeader";
            }

            @Override
            public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(ProvisionResource definition) {
                var deprovisionEndpoint = definition.getProperty("deprovisionEndpoint");
                return httpClient.executeAsync(new Request.Builder().url((String) deprovisionEndpoint).build(), emptyList())
                        .thenCompose(response -> {
                            if (response.isSuccessful()) {
                                return completedFuture(StatusResult.success(DeprovisionedResource.Builder.from(definition).build()));
                            } else {
                                return failedFuture(new EdcException("Deprovision failed"));
                            }
                        });
            }
        }

        private static class AddHeaderProvisioner implements Provisioner {

            @Override
            public String supportedType() {
                return "AddHeader";
            }

            @Override
            public CompletableFuture<StatusResult<ProvisionedResource>> provision(ProvisionResource provisionResource) {
                var provisionedResource = ProvisionedResource.Builder.from(provisionResource)
                        .dataAddress(DataAddress.Builder.newInstance()
                                .properties(provisionResource.getDataAddress().getProperties())
                                .type("HttpData")
                                .property("header:provisionHeader", "value")
                                .build())
                        .build();
                return completedFuture(StatusResult.success(provisionedResource));
            }

        }

        private static class AddHeaderResourceGenerator implements ResourceDefinitionGenerator {

            @Override
            public String supportedType() {
                return "CustomProvisionType";
            }

            @Override
            public ProvisionResource generate(DataFlow dataFlow) {
                return ProvisionResource.Builder
                        .newInstance()
                        .flowId(dataFlow.getId())
                        .dataAddress(dataFlow.getSource())
                        .type("AddHeader")
                        .property("deprovisionEndpoint", "http://localhost:%d/deprovision".formatted(SOURCE_BACKEND_PORT))
                        .build();
            }
        }

        private static class AsyncResourceGenerator implements ResourceDefinitionGenerator {

            @Override
            public String supportedType() {
                return "CustomProvisionType";
            }

            @Override
            public ProvisionResource generate(DataFlow dataFlow) {
                return ProvisionResource.Builder
                        .newInstance()
                        .flowId(dataFlow.getId())
                        .type("AsyncResource")
                        .build();
            }
        }

        /**
         * Fake Async provisioner that schedules the response delivery after 2 seconds
         */
        private static class AsyncProvisioner implements Provisioner {

            private final DataPlaneManager dataPlaneManager;

            AsyncProvisioner(DataPlaneManager dataPlaneManager) {
                this.dataPlaneManager = dataPlaneManager;
            }

            @Override
            public String supportedType() {
                return "AsyncResource";
            }

            @Override
            public CompletableFuture<StatusResult<ProvisionedResource>> provision(ProvisionResource provisionResource) {
                Executors.newScheduledThreadPool(1).schedule(() -> {
                    var actualProvisionedResource = ProvisionedResource.Builder.from(provisionResource).build();
                    return dataPlaneManager.resourceProvisioned(actualProvisionedResource);
                }, 1, SECONDS);
                var asyncProvisionedResource = ProvisionedResource.Builder.from(provisionResource).pending(true).build();
                return completedFuture(StatusResult.success(asyncProvisionedResource));
            }
        }

        private class AsyncDeprovisioner implements Deprovisioner {

            @Override
            public String supportedType() {
                return "AsyncResource";
            }

            @Override
            public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(ProvisionResource provisionResource) {
                Executors.newScheduledThreadPool(1).schedule(() -> {
                    var actualProvisionedResource = DeprovisionedResource.Builder.from(provisionResource).build();
                    return dataPlaneManager.resourceDeprovisioned(actualProvisionedResource);
                }, 2, SECONDS);
                var resource = DeprovisionedResource.Builder.from(provisionResource).pending(true).build();
                return completedFuture(StatusResult.success(resource));
            }
        }
    }

    abstract static class Tests extends TransferEndToEndTestBase {

        @RegisterExtension
        static WireMockExtension source = WireMockExtension.newInstance()
                .options(wireMockConfig().port(SOURCE_BACKEND_PORT))
                .build();

        @RegisterExtension
        static WireMockExtension destination = WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .build();

        @Test
        void shouldExecuteProviderProvisioningAndDeprovisioning(@Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                                @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider) {

            source.stubFor(get("/source").willReturn(ok("data")));
            source.stubFor(get("/deprovision").willReturn(ok()));

            destination.stubFor(post(anyUrl()).willReturn(ok()));

            var assetId = UUID.randomUUID().toString();
            var sourceDataAddress = Map.<String, Object>of(
                    EDC_NAMESPACE + "name", "transfer-test",
                    EDC_NAMESPACE + "baseUrl", "http://localhost:%d/source".formatted(source.getPort()),
                    EDC_NAMESPACE + "type", "CustomProvisionType"
            );

            createResourcesOnProvider(provider, assetId, sourceDataAddress);

            var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("HttpData-PUSH")
                    .withDestination(createObjectBuilder()
                            .add(TYPE, EDC_NAMESPACE + "DataAddress")
                            .add(EDC_NAMESPACE + "type", "HttpData")
                            .add(EDC_NAMESPACE + "baseUrl", "http://localhost:%d/destination".formatted(destination.getPort()))
                            .build()
                    )
                    .execute();

            consumer.awaitTransferToBeInState(consumerTransferProcessId, COMPLETED);

            source.verify(getRequestedFor(anyUrl()).withHeader("provisionHeader", equalTo("value")));

            var providerTransferProcessId = provider.getTransferProcesses().stream()
                    .filter(filter -> filter.asJsonObject().getString("correlationId").equals(consumerTransferProcessId))
                    .map(id -> id.asJsonObject().getString("@id")).findFirst().orElseThrow();

            await().untilAsserted(() -> source.verify(getRequestedFor(urlEqualTo("/deprovision"))));
            await().untilAsserted(() -> {
                var dataFlow = providerDataPlaneStore().findById(providerTransferProcessId);
                assertThat(dataFlow).isNotNull().extracting(StatefulEntity::getState).isEqualTo(DataFlowStates.DEPROVISIONED.code());
            });

        }

        protected abstract DataPlaneStore providerDataPlaneStore();
    }

    @Nested
    @EndToEndTest
    class EmbeddedDataPlaneInMemory extends Tests {

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_CP)
                .modules(Runtimes.ControlPlane.EMBEDDED_DP_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .configurationProvider(Runtimes.DataPlane::config)
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_CP)
                .modules(Runtimes.ControlPlane.EMBEDDED_DP_MODULES)
                .endpoints(PROVIDER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .configurationProvider(Runtimes.DataPlane::config)
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build()
                .registerSystemExtension(ServiceExtension.class, new TestProviderProvisionerExtension());


        @Override
        protected DataPlaneStore providerDataPlaneStore() {
            return PROVIDER_CONTROL_PLANE.getService(DataPlaneStore.class);
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        static final Endpoints CONSUMER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_CP)
                .modules(Runtimes.ControlPlane.MODULES)
                .endpoints(CONSUMER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_CP)
                .modules(Runtimes.ControlPlane.MODULES)
                .endpoints(PROVIDER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        @RegisterExtension
        static final RuntimeExtension CONSUMER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_DP)
                .modules(Runtimes.DataPlane.IN_MEM_MODULES)
                .endpoints(Runtimes.DataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.DataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.dataPlaneSelectorFor(CONSUMER_ENDPOINTS))
                .build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.DataPlane.IN_MEM_MODULES)
                .endpoints(Runtimes.DataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.DataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.dataPlaneSelectorFor(PROVIDER_ENDPOINTS))
                .build()
                .registerSystemExtension(ServiceExtension.class, new TestProviderProvisionerExtension());

        @Override
        protected DataPlaneStore providerDataPlaneStore() {
            return PROVIDER_DATA_PLANE.getService(DataPlaneStore.class);
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        static final String CONSUMER_DB = "consumer";
        static final String PROVIDER_DB = "provider";

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback CREATE_DATABASES = context -> {
            POSTGRESQL_EXTENSION.createDatabase(CONSUMER_DB);
            POSTGRESQL_EXTENSION.createDatabase(PROVIDER_DB);
        };

        static final Endpoints CONSUMER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_CP)
                .modules(Runtimes.ControlPlane.MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(CONSUMER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(CONSUMER_DB))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        @RegisterExtension
        static final RuntimeExtension CONSUMER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_DP)
                .modules(Runtimes.DataPlane.SQL_MODULES)
                .endpoints(Runtimes.DataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.DataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.dataPlaneSelectorFor(CONSUMER_ENDPOINTS))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(CONSUMER_DB))
                .build();

        static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_CP)
                .modules(Runtimes.ControlPlane.MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(PROVIDER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(PROVIDER_DB))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        @RegisterExtension
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.DataPlane.SQL_MODULES)
                .endpoints(Runtimes.DataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.DataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.dataPlaneSelectorFor(PROVIDER_ENDPOINTS))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(PROVIDER_DB))
                .build()
                .registerSystemExtension(ServiceExtension.class, new TestProviderProvisionerExtension());

        @Override
        protected DataPlaneStore providerDataPlaneStore() {
            return PROVIDER_DATA_PLANE.getService(DataPlaneStore.class);
        }
    }

}
