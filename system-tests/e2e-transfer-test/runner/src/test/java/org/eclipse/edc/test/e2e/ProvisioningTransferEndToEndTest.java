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

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResourceDefinition;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Provisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGenerator;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockserver.model.HttpRequest.request;

public class ProvisioningTransferEndToEndTest {

    private static final TransferEndToEndParticipant CONSUMER = TransferEndToEndParticipant.Builder.newInstance()
            .name("consumer")
            .id("urn:connector:consumer")
            .build();
    private static final TransferEndToEndParticipant PROVIDER = TransferEndToEndParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();

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
    void shouldExecuteConsumerProvisioning() {
        var source = ClientAndServer.startClientAndServer(getFreePort());
        source.when(request()).respond(HttpResponse.response("data"));
        var destination = ClientAndServer.startClientAndServer(getFreePort());
        destination.when(request()).respond(HttpResponse.response());

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

        @Override
        public void initialize(ServiceExtensionContext context) {
            resourceDefinitionGeneratorManager.registerConsumerGenerator(new ResourceDefinitionGenerator() {

                @Override
                public String supportedType() {
                    return "HttpData";
                }

                @Override
                public ProvisionResourceDefinition generate(DataFlow dataFlow) {
                    return ProvisionResourceDefinition.Builder
                            .newInstance()
                            .dataAddress(dataFlow.getDestination())
                            .type("AddHeader")
                            .build();
                }
            });

            provisionerManager.register(new Provisioner() {
                @Override
                public String supportedType() {
                    return "AddHeader";
                }

                @Override
                public CompletableFuture<StatusResult<ProvisionedResource>> provision(ProvisionResourceDefinition provisionResourceDefinition) {
                    var provisionedResource = ProvisionedResource.Builder.newInstance()
                            .dataAddress(DataAddress.Builder.newInstance()
                                    .properties(provisionResourceDefinition.getDataAddress().getProperties())
                                    .property("header:provisionHeader", "value")
                                    .build())
                            .build();
                    return CompletableFuture.completedFuture(StatusResult.success(provisionedResource));
                }

            });
        }
    }
}
