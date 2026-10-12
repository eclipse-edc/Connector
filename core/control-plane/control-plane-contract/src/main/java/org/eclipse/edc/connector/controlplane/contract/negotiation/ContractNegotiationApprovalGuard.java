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

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;

/**
 * Holds provider negotiations in {@code REQUESTED} state whose contract policy is not fulfilled in the
 * {@link ApprovalContractNegotiationPolicyContext#APPROVAL_SCOPE} scope, so that they need to be approved manually.
 * <p>
 * The counterparty is represented by a {@link ParticipantAgent} rebuilt from the negotiation's counterparty id and the
 * claims captured when the contract request was received. The negotiated {@link Asset} and the {@link ContractDefinition}
 * that offered it are looked up and passed to the policy context, null if they are not available anymore.
 */
public class ContractNegotiationApprovalGuard implements ContractNegotiationPendingGuard {

    private final PolicyEngine policyEngine;
    private final AssetIndex assetIndex;
    private final ContractDefinitionStore contractDefinitionStore;
    private final Monitor monitor;

    public ContractNegotiationApprovalGuard(PolicyEngine policyEngine, AssetIndex assetIndex, ContractDefinitionStore contractDefinitionStore, Monitor monitor) {
        this.policyEngine = policyEngine;
        this.assetIndex = assetIndex;
        this.contractDefinitionStore = contractDefinitionStore;
        this.monitor = monitor;
    }

    @Override
    public boolean test(ContractNegotiation negotiation) {
        if (negotiation.getType() != PROVIDER || negotiation.getState() != REQUESTED.code()) {
            return false;
        }

        var offer = negotiation.getLastContractOffer();
        if (offer == null) {
            return false;
        }

        var claims = negotiation.getClaims() == null ? Map.<String, Object>of() : negotiation.getClaims();
        var agent = new ParticipantAgent(negotiation.getCounterPartyId(), claims, Map.of());
        var participantContextId = negotiation.getParticipantContextId();
        var context = new ApprovalContractNegotiationPolicyContext(participantContextId, agent,
                findAsset(participantContextId, offer), findContractDefinition(participantContextId, offer));
        var result = policyEngine.evaluate(offer.getPolicy(), context);
        if (result.failed()) {
            monitor.debug(() -> "Contract negotiation %s needs manual approval: %s".formatted(negotiation.getId(), result.getFailureDetail()));
            return true;
        }
        return false;
    }

    private @Nullable Asset findAsset(String participantContextId, ContractOffer offer) {
        if (participantContextId == null || offer.getAssetId() == null) {
            return null;
        }
        return assetIndex.findById(participantContextId, offer.getAssetId());
    }

    private @Nullable ContractDefinition findContractDefinition(String participantContextId, ContractOffer offer) {
        if (participantContextId == null || offer.getId() == null) {
            return null;
        }
        // the offer id of a provider negotiation is built from the id of the contract definition that offered the asset
        return ContractOfferId.parseId(offer.getId())
                .map(offerId -> contractDefinitionStore.findById(participantContextId, offerId.definitionPart()))
                .orElse(failure -> null);
    }
}
