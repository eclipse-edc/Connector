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

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.policy.ParticipantAgentPolicyContext;

import java.time.Instant;

/**
 * Policy Context for "transfer.process" scope
 */
public class TransferProcessPolicyContext extends PolicyContextImpl implements AgreementPolicyContext, ParticipantAgentPolicyContext {

    @PolicyScope
    public static final String TRANSFER_SCOPE = "transfer.process";

    private final ParticipantAgent agent;
    private final ContractAgreement agreement;
    private final Instant now;

    public TransferProcessPolicyContext(ParticipantAgent agent, ContractAgreement agreement, Instant now) {
        this.agent = agent;
        this.agreement = agreement;
        this.now = now;
    }

    @Override
    public ParticipantAgent participantAgent() {
        return agent;
    }

    @Override
    public Instant now() {
        return now;
    }

    @Override
    public ContractAgreement contractAgreement() {
        return agreement;
    }

    @Override
    public String scope() {
        return TRANSFER_SCOPE;
    }
}
