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

package org.eclipse.edc.test.e2e;

import okhttp3.Request;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Deprovisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Provisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGenerator;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static jakarta.json.Json.createObjectBuilder;
import static java.util.Collections.emptyList;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ProvisioningTransferEndToEndTest {

    private static final TransferEndToEndParticipant CONSUMER = TransferEndToEndParticipant.Builder.newInstance()
            .name("consumer")
            .id("urn:connector:consumer")
            .build();
    private static final TransferEndToEndParticipant PROVIDER = TransferEndToEndParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();
    private static final int DESTINATION_BACKEND_PORT = getFreePort();

    @RegisterExtension
    @Order(0)
    private final RuntimeExtension consumerControlPlane = new RuntimePerMethodExtension(
            Runtimes.IN_MEMORY_CONTROL_PLANE_EMBEDDED_DATA_PLANE.create("consumer-control-plane")
                    .configurationProvider(CONSUMER::controlPlaneEmbeddedDataPlaneConfig)
                    .registerSystemExtension(ServiceExtension.class, new RegisterConsumerResourceDefinitionGenerator())
    );

    @RegisterExtension
    @Order(0)
    private final RuntimeExtension providerControlPlane = new RuntimePerMethodExtension(
            Runtimes.IN_MEMORY_CONTROL_PLANE_EMBEDDED_DATA_PLANE.create("provider-control-plane")
                    .configurationProvider(PROVIDER::controlPlaneEmbeddedDataPlaneConfig)
    );

    @Test
    void shouldExecuteConsumerProvisioningAndDeprovisioning() {
        var source = ClientAndServer.startClientAndServer(getFreePort());
        source.when(request("/source")).respond(response("data"));
        var destination = ClientAndServer.startClientAndServer(DESTINATION_BACKEND_PORT);
        destination.when(request()).respond(response());
        destination.when(request("/deprovision")).respond(response());

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

        destination.verify(request().withHeader("provisionHeader", "value"));

        await().untilAsserted(() -> {
            destination.verify(request("/deprovision"));
        });

        source.stop();
        destination.stop();
    }

    protected void createResourcesOnProvider(String assetId, Map<String, Object> dataAddressProperties) {
        PROVIDER.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var noConstraintPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, noConstraintPolicyId);
    }

    private static class RegisterConsumerResourceDefinitionGenerator implements ServiceExtension {

        @Inject
        private ResourceDefinitionGeneratorManager resourceDefinitionGeneratorManager;

        @Inject
        private ProvisionerManager provisionerManager;

        @Inject
        private EdcHttpClient httpClient;

        @Override
        public void initialize(ServiceExtensionContext context) {
            resourceDefinitionGeneratorManager.registerConsumerGenerator(new AddHeaderResourceGenerator());

            provisionerManager.register(new AddHeaderProvisioner());
            provisionerManager.register(new CallEndpointDeprovisioner(httpClient));
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
                                return CompletableFuture.completedFuture(StatusResult.success(DeprovisionedResource.Builder.from(definition).build()));
                            } else {
                                return CompletableFuture.failedFuture(new EdcException("Deprovision failed"));
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
                return CompletableFuture.completedFuture(StatusResult.success(provisionedResource));
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
                        .dataAddress(dataFlow.getDestination())
                        .type("AddHeader")
                        .property("deprovisionEndpoint", "http://localhost:%d/deprovision".formatted(DESTINATION_BACKEND_PORT))
                        .build();
            }
        }
    }
}
