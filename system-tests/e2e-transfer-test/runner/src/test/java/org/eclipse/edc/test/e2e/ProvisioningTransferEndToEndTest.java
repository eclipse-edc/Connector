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
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGenerator;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ProvisioningTransferEndToEndTest {

    private static final TransferEndToEndParticipant CONSUMER = TransferEndToEndParticipant.Builder.newInstance()
            .name("consumer")
            .id("urn:connector:consumer")
            .build();
    private static final TransferEndToEndParticipant PROVIDER = TransferEndToEndParticipant.Builder.newInstance()
            .name("provider")
            .id("urn:connector:provider")
            .build();

    private final ResourceDefinitionGenerator consumerResourceDefinitionGenerator = spy(new ConsumerResourceDefinitionGenerator());

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
        var assetId = UUID.randomUUID().toString();
        var sourceDataAddress = Map.<String, Object>of(
                EDC_NAMESPACE + "name", "transfer-test",
                EDC_NAMESPACE + "baseUrl", "http://any/source",
                EDC_NAMESPACE + "type", "HttpData"
        );

        createResourcesOnProvider(assetId, sourceDataAddress);

        var consumerTransferProcessId = CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("HttpData-PUSH")
                .withDestination(createObjectBuilder()
                        .add(TYPE, EDC_NAMESPACE + "DataAddress")
                        .add(EDC_NAMESPACE + "type", "HttpData")
                        .add(EDC_NAMESPACE + "baseUrl", "http://localhost:9999/any")
                        .build()
                )
                .execute();

        CONSUMER.awaitTransferToBeInState(consumerTransferProcessId, STARTED);

        verify(consumerResourceDefinitionGenerator).generate(any());
    }

    protected void createResourcesOnProvider(String assetId, Map<String, Object> dataAddressProperties) {
        PROVIDER.createAsset(assetId, Map.of("description", "description"), dataAddressProperties);
        var noConstraintPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, noConstraintPolicyId);
    }

    private static class ConsumerResourceDefinitionGenerator implements ResourceDefinitionGenerator {

        @Override
        public String supportedType() {
            return "HttpData";
        }

        @Override
        public ProvisionResourceDefinition generate(DataFlow dataFlow) {
            var dataAddress = DataAddress.Builder.newInstance().type("HttpData")
                    .property(EDC_NAMESPACE + "baseUrl", "http://localhost:11111/any").build();
            return ProvisionResourceDefinition.Builder.newInstance()
                    .dataAddress(dataAddress)
                    .build();
        }
    }

    private class RegisterConsumerResourceDefinitionGenerator implements ServiceExtension {

        @Inject
        private ResourceDefinitionGeneratorManager resourceDefinitionGeneratorManager;

        @Override
        public void initialize(ServiceExtensionContext context) {
            resourceDefinitionGeneratorManager.registerConsumerGenerator(consumerResourceDefinitionGenerator);
        }
    }
}
