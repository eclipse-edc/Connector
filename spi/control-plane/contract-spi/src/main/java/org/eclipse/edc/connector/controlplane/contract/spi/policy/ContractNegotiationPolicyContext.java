/*
 *  Copyright (c) 2024 Cofinity-X
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

package org.eclipse.edc.connector.controlplane.contract.spi.policy;

import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.eclipse.edc.spi.agent.ParticipantAgent;

/**
 * Policy Context for "contract.negotiation" scope
 */
public class ContractNegotiationPolicyContext extends PolicyContextImpl {

    @PolicyScope
    public static final String NEGOTIATION_SCOPE = "contract.negotiation";

    private final ParticipantAgent agent;

    public ContractNegotiationPolicyContext(ParticipantAgent agent) {
        this.agent = agent;
    }

    public ParticipantAgent agent() {
        return agent;
    }

    @Override
    public String scope() {
        return NEGOTIATION_SCOPE;
    }
}
