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

package org.eclipse.edc.connector.controlplane.contract;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuardRegistry;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolRemoteMessageDispatcher;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ContractCoreExtensionTest {

    private final PolicyEngine policyEngine = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(AssetIndex.class, mock());
        context.registerService(PolicyEngine.class, policyEngine);
        context.registerService(EventRouter.class, mock());
        context.registerService(TypeManager.class, mock());
        context.registerService(ContractNegotiationObservable.class, mock());
        context.registerService(ProtocolWebhookResolver.class, mock());
        context.registerService(ContractNegotiationStore.class, mock());
        context.registerService(ParticipantIdentityResolver.class, mock());
        context.registerService(ProtocolRemoteMessageDispatcher.class, mock());
        when(policyEngine.evaluate(any(), any())).thenReturn(Result.failure("approval needed"));
    }

    @Test
    void shouldRegisterApprovalGuard_whenEnabled(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("edc.negotiation.approval.enabled", "true")));
        var extension = objectFactory.constructInstance(ContractCoreExtension.class);

        extension.initialize(context);

        var registry = context.getService(ContractNegotiationPendingGuardRegistry.class);
        assertThat(registry.test(requestedProviderNegotiation())).isTrue();
    }

    @Test
    void shouldNotRegisterApprovalGuard_byDefault(ServiceExtensionContext context, ObjectFactory objectFactory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of()));
        var extension = objectFactory.constructInstance(ContractCoreExtension.class);

        extension.initialize(context);

        var registry = context.getService(ContractNegotiationPendingGuardRegistry.class);
        assertThat(registry.test(requestedProviderNegotiation())).isFalse();
    }

    private ContractNegotiation requestedProviderNegotiation() {
        return ContractNegotiation.Builder.newInstance()
                .id("id")
                .type(PROVIDER)
                .state(REQUESTED.code())
                .participantContextId("participantContextId")
                .counterPartyId("counter-party")
                .counterPartyAddress("https://counter-party")
                .protocol("protocol")
                .contractOffer(ContractOffer.Builder.newInstance().id("offer").assetId("asset").policy(Policy.Builder.newInstance().build()).build())
                .build();
    }
}
