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

package org.eclipse.edc.connector.controlplane.contract.spi.policy;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.jetbrains.annotations.Nullable;

/**
 * Policy Context for "approval.contract.negotiation" scope. The contract policy of a provider negotiation in
 * {@code REQUESTED} state is evaluated in this scope to decide if it can be approved automatically or if it needs a manual
 * approval.
 * <p>
 * The scope deliberately does not start with {@link ContractNegotiationPolicyContext#NEGOTIATION_SCOPE}, so that only the
 * constraints explicitly bound to it are evaluated.
 * <p>
 * Besides the counter-party, the context carries the negotiated {@link Asset} and the {@link ContractDefinition} that
 * offered it, so that approval rules can be specific to an asset or generic for a contract definition.
 */
public class ApprovalContractNegotiationPolicyContext extends ContractNegotiationPolicyContext
        implements AssetPolicyContext, ContractDefinitionPolicyContext {

    @PolicyScope
    public static final String APPROVAL_SCOPE = "approval.contract.negotiation";

    private final Asset asset;
    private final ContractDefinition contractDefinition;

    public ApprovalContractNegotiationPolicyContext(String participantContextId, ParticipantAgent agent) {
        this(participantContextId, agent, null, null);
    }

    public ApprovalContractNegotiationPolicyContext(String participantContextId, ParticipantAgent agent,
                                                    @Nullable Asset asset, @Nullable ContractDefinition contractDefinition) {
        super(participantContextId, agent);
        this.asset = asset;
        this.contractDefinition = contractDefinition;
    }

    @Override
    public @Nullable Asset asset() {
        return asset;
    }

    @Override
    public @Nullable ContractDefinition contractDefinition() {
        return contractDefinition;
    }

    @Override
    public String scope() {
        return APPROVAL_SCOPE;
    }
}
