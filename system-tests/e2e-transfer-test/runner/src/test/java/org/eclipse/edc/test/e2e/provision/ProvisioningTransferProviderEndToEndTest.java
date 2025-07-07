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
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.test.e2e.Runtimes;
import org.eclipse.edc.test.e2e.TransferEndToEndParticipant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static jakarta.json.Json.createObjectBuilder;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ProvisioningTransferProviderEndToEndTest {

    private static final TransferEndToEndParticipant CONSUMER = TransferEndToEndParticipant.Builder.newInstance()
            .name("consumer")
            .id("urn:connector:consumer")
            .build();
    private static final TransferEndToEndParticipant PROVIDER = TransferEndToEndParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();

    private static final int SOURCE_BACKEND_PORT = getFreePort();

    @Nested
    class EmbeddedDataPlaneInMemory extends Tests {
        @RegisterExtension
        @Order(0)
        private final RuntimeExtension consumerControlPlane = new RuntimePerMethodExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE_EMBEDDED_DATA_PLANE.create("consumer-control-plane")
                        .configurationProvider(CONSUMER::controlPlaneEmbeddedDataPlaneConfig)
        );

        @RegisterExtension
        @Order(0)
        private final RuntimeExtension providerControlPlane = new RuntimePerMethodExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE_EMBEDDED_DATA_PLANE.create("provider-control-plane")
                        .configurationProvider(PROVIDER::controlPlaneEmbeddedDataPlaneConfig)
                        .registerSystemExtension(ServiceExtension.class, new TestProviderProvisionerExtension())
        );

        @Override
        protected DataPlaneStore providerDataPlaneStore() {
            return providerControlPlane.getService(DataPlaneStore.class);
        }
    }

    @Nested
    class InMemory extends Tests {

        @RegisterExtension
        @Order(0)
        private final RuntimeExtension consumerControlPlane = new RuntimePerMethodExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE.create("consumer-control-plane")
                        .configurationProvider(CONSUMER::controlPlaneConfig)
        );

        @RegisterExtension
        @Order(1)
        private final RuntimeExtension consumerDataPlane = new RuntimePerMethodExtension(
                Runtimes.IN_MEMORY_DATA_PLANE.create("consumer-data-plane")
                        .configurationProvider(CONSUMER::dataPlaneConfig)
        );

        @RegisterExtension
        @Order(0)
        private final RuntimeExtension providerControlPlane = new RuntimePerMethodExtension(
                Runtimes.IN_MEMORY_CONTROL_PLANE.create("provider-control-plane")
                        .configurationProvider(PROVIDER::controlPlaneConfig)
        );

        @RegisterExtension
        @Order(1)
        private final RuntimeExtension providerDataPlane = new RuntimePerMethodExtension(
                Runtimes.IN_MEMORY_DATA_PLANE.create("provider-data-plane")
                        .configurationProvider(PROVIDER::dataPlaneConfig)
                        .registerSystemExtension(ServiceExtension.class, new TestProviderProvisionerExtension())
        );

        @Override
        protected DataPlaneStore providerDataPlaneStore() {
            return providerDataPlane.getService(DataPlaneStore.class);
        }
    }

    abstract class Tests {
        @Test
        void shouldExecuteConsumerProvisioningAndDeprovisioning() {
            var source = ClientAndServer.startClientAndServer(SOURCE_BACKEND_PORT);
            source.when(request("/source")).respond(response("data"));
            source.when(request("/deprovision")).respond(response());
            var destination = ClientAndServer.startClientAndServer(getFreePort());
            destination.when(request()).respond(response());

            var assetId = UUID.randomUUID().toString();
            var sourceDataAddress = Map.<String, Object>of(
                    EDC_NAMESPACE + "name", "transfer-test",
                    EDC_NAMESPACE + "baseUrl", "http://localhost:%d/source".formatted(source.getPort()),
                    EDC_NAMESPACE + "type", "HttpData"
            );

            createResourcesOnProvider(assetId, sourceDataAddress);

            var consumerTransferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                    .withTransferType("HttpData-PUSH")
                    .withDestination(createObjectBuilder()
                            .add(TYPE, EDC_NAMESPACE + "DataAddress")
                            .add(EDC_NAMESPACE + "type", "HttpData")
                            .add(EDC_NAMESPACE + "baseUrl", "http://localhost:%d/destination".formatted(destination.getPort()))
                            .build()
                    )
                    .execute();

            CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, COMPLETED);

            source.verify(request().withHeader("provisionHeader", "value"));

            var providerTransferProcessId = PROVIDER.getTransferProcesses().stream()
                    .filter(filter -> filter.asJsonObject().getString("correlationId").equals(consumerTransferProcessId))
                    .map(id -> id.asJsonObject().getString("@id")).findFirst().orElseThrow();

            await().untilAsserted(() -> source.verify(request("/deprovision")));
            await().untilAsserted(() -> {
                var dataFlow = providerDataPlaneStore().findById(providerTransferProcessId);
                assertThat(dataFlow).isNotNull().extracting(StatefulEntity::getState).isEqualTo(DataFlowStates.DEPROVISIONED.code());
            });

            source.stop();
            destination.stop();
        }

        protected abstract DataPlaneStore providerDataPlaneStore();
    }

    private void createResourcesOnProvider(String assetId, Map<String, Object> dataAddressProperties) {
        PROVIDER.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var noConstraintPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, noConstraintPolicyId);
    }

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
                                .property("header:provisionHeader", "value")
                                .build())
                        .build();
                return completedFuture(StatusResult.success(provisionedResource));
            }

        }

        private static class AddHeaderResourceGenerator implements ResourceDefinitionGenerator {

            @Override
            public String supportedType() {
                return "HttpData";
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
                return "HttpData";
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

}
