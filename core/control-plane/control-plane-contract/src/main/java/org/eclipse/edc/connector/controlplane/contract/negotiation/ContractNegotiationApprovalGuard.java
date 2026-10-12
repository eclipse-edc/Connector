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

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ApprovalContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;

/**
 * Holds provider negotiations in {@code REQUESTED} state whose contract policy is not fulfilled in the
 * {@link ApprovalContractNegotiationPolicyContext#APPROVAL_SCOPE} scope, so that they need to be approved manually.
 * <p>
 * The counterparty is represented by a {@link ParticipantAgent} rebuilt from the negotiation's counterparty id and the
 * claims captured when the contract request was received.
 */
public class ContractNegotiationApprovalGuard implements ContractNegotiationPendingGuard {

    private final PolicyEngine policyEngine;
    private final Monitor monitor;

    public ContractNegotiationApprovalGuard(PolicyEngine policyEngine, Monitor monitor) {
        this.policyEngine = policyEngine;
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
        var context = new ApprovalContractNegotiationPolicyContext(negotiation.getParticipantContextId(), agent);
        var result = policyEngine.evaluate(offer.getPolicy(), context);
        if (result.failed()) {
            monitor.debug(() -> "Contract negotiation %s needs manual approval: %s".formatted(negotiation.getId(), result.getFailureDetail()));
            return true;
        }
        return false;
    }
}
