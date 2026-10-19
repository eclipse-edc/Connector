/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.tck.dps.virtual;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.dataplane.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
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

import static org.eclipse.edc.tck.dps.virtual.VirtualDpsTckExtension.NAME;

/**
 * Sets up the virtual runtime for the DPS TCK: creates the participant context, registers the TCK as data plane, seeds
 * the asset and the contract agreement used by the TCK, and registers the webhook the TCK uses to trigger transfers.
 */
@Extension(NAME)
public class VirtualDpsTckExtension implements ServiceExtension {

    public static final String NAME = "Virtual DPS TCK Setup";
    /**
     * The dataspace profile the TCK speaks: on the virtual runtime the protocol of an entity is the profile id.
     */
    public static final String DATASPACE_PROFILE = "http-dsp-profile-2025-1";
    private static final String CONTEXT = "tck";

    @Setting(description = "Configures the participant context id for the tck suite runtime", key = "edc.participant.context.id", defaultValue = "participantContextId")
    public String participantContextId;
    @Setting(description = "Base URL of the TCK acting as data plane, equal to dataspacetck.callback.address", key = "edc.tck.dataplane.url")
    public String tckDataPlaneUrl;
    @Configuration
    private TckWebhookApiConfiguration webhookApiConfiguration;
    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private WebService webService;
    @Inject
    private TransferProcessService transferProcessService;
    @Inject
    private DataPlaneSelectorService dataPlaneSelectorService;
    @Inject
    private ParticipantContextService participantContextService;
    @Inject
    private ParticipantContextConfigService participantContextConfigService;
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
        webService.registerResource(CONTEXT, new VirtualDpsTckWebhookController(transferProcessService, participantContextService,
                participantContextId, monitor));
    }

    @Override
    public void prepare() {
        // the data is seeded once the stores are ready, i.e. after the SQL schemas have been created
        participantContextService.createParticipantContext(ParticipantContext.Builder.newInstance()
                        .id(participantContextId)
                        .identity(participantContextId)
                        .build())
                .orElseThrow(f -> new EdcException("Failed to create participant context: " + f.getFailureDetail()));
        participantContextConfigService.save(ParticipantContextConfiguration.Builder.newInstance()
                        .participantContextId(participantContextId)
                        .entries(Map.of("edc.participant.id", participantContextId))
                        .build())
                .orElseThrow(f -> new EdcException("Failed to save participant context configuration: " + f.getFailureDetail()));

        dataPlaneSelectorService.register(DataPlaneInstance.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .url(tckDataPlaneUrl + "/dataflows")
                        .allowedTransferTypes(Set.of("HttpData-PULL", "HttpData-PUSH"))
                        .participantContextId(participantContextId)
                        .build())
                .orElseThrow(f -> new EdcException("Failed to register TCK data plane: " + f.getFailureDetail()));

        var asset = Asset.Builder.newInstance()
                .participantContextId(participantContextId)
                .build();
        assetService.create(asset)
                .orElseThrow(f -> new EdcException("Failed to create asset: " + f.getFailureDetail()));

        contractNegotiationStore.save(ContractNegotiation.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .participantContextId(participantContextId)
                        .counterPartyId(UUID.randomUUID().toString())
                        .counterPartyAddress("http://localhost/test")
                        .protocol(DATASPACE_PROFILE)
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
                        .build())
                .orElseThrow(f -> new EdcException("Failed to save contract negotiation: " + f.getFailureDetail()));
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
