/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dps.signaling;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.edc.tck.dps.signaling.DpsTckExtension.NAME;

/**
 * Registers a webhook endpoint on the "tck" context that the TCK uses to trigger
 * DataFlowPrepareMessage dispatching via a real TransferProcess.
 */
@Extension(NAME)
public class DpsTckExtension implements ServiceExtension {

    public static final String NAME = "DPS TCK Signaling Trigger";

    private static final String CONTEXT = "tck";

    @Configuration
    private TckWebhookApiConfiguration webhookApiConfiguration;
    @Setting(description = "Configures the participant context id for the tck suite runtime", key = "edc.participant.context.id", defaultValue = "participantContextId")
    public String participantContextId;

    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private WebService webService;
    @Inject
    private TransferProcessService transferProcessService;
    @Inject
    private DataPlaneSelectorService dataPlaneSelectorService;
    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;
    @Inject
    private ContractNegotiationStore contractNegotiationStore;
    @Inject
    private AssetService assetService;
    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        portMappingRegistry.register(new PortMapping(CONTEXT, webhookApiConfiguration.port(), webhookApiConfiguration.path()));
        webService.registerResource(CONTEXT, new DpsTckWebhookController(
                transferProcessService, participantContextSupplier, monitor));

        // TODO: should this API be tested as well in the TCK?
        var dataPlaneInstance = DataPlaneInstance.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .url("http://localhost:8083/dataflows") // TODO: configuration
                .allowedTransferType(Set.of("HttpData-PULL", "HttpData-PUSH"))
                .participantContextId(participantContextId)
                .build();

        dataPlaneSelectorService.register(dataPlaneInstance)
                .orElseThrow(f -> new EdcException("Failed to register TCK data plane: " + f.getFailureDetail()));

        var asset = Asset.Builder.newInstance().build();
        assetService.create(asset);

        contractNegotiationStore.save(ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .participantContextId(participantContextId)
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("http://localhost/test")
                .protocol("dataspace-protocol-http:2025-1")
                .state(ContractNegotiationStates.FINALIZED.code())
                .contractAgreement(ContractAgreement.Builder.newInstance()
                        .participantContextId(participantContextId)
                        .id("ADPS0101")
                        .agreementId("ADPS0101")
                        .providerId("providerId")
                        .consumerId("consumerId")
                        .claims(Map.of("key", "value"))
                        .assetId(asset.getId())
                        .policy(Policy.Builder.newInstance().assignee("providerId").assigner("consumerId").build())
                        .build())
                .build());
    }

    @Settings
    record TckWebhookApiConfiguration(
            @Setting(key = "web.http." + CONTEXT + ".port", description = "Port for " + CONTEXT + " api context", defaultValue = DEFAULT_PORT + "")
            int port,
            @Setting(key = "web.http." + CONTEXT + ".path", description = "Path for " + CONTEXT + " api context", defaultValue = DEFAULT_PATH)
            String path
    ) {

        private static final String DEFAULT_PATH = "/tck";
        private static final int DEFAULT_PORT = 8687;

    }
}
