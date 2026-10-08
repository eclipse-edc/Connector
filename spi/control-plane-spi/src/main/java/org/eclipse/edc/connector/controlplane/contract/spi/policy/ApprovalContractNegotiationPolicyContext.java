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

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.engine.spi.PolicyScope;

/**
 * Policy Context for "approval.contract.negotiation" scope. The contract policy of a provider negotiation in
 * {@code REQUESTED} state is evaluated in this scope to decide if it can be approved automatically or if it needs a manual
 * approval.
 * <p>
 * The scope deliberately does not start with {@link ContractNegotiationPolicyContext#NEGOTIATION_SCOPE}, so that only the
 * constraints explicitly bound to it are evaluated.
 */
public class ApprovalContractNegotiationPolicyContext extends ContractNegotiationPolicyContext {

    @PolicyScope
    public static final String APPROVAL_SCOPE = "approval.contract.negotiation";

    public ApprovalContractNegotiationPolicyContext(String participantContextId, ParticipantAgent agent) {
        super(participantContextId, agent);
    }

    @Override
    public String scope() {
        return APPROVAL_SCOPE;
    }
}
