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

package org.eclipse.edc.connector.policy.monitor.spi;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.AgreementPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.ParticipantContextPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyScope;

import java.time.Instant;
import java.util.Map;

/**
 * Policy Context for "policy.monitor" scope. The counterparty {@link ParticipantAgent} is rebuilt from the agreement:
 * its identity is the consumer id and its claims are the ones snapshotted on the agreement when it was reached.
 * Attributes are not available in this scope.
 */
public class PolicyMonitorContext extends PolicyContextImpl implements AgreementPolicyContext, ParticipantAgentPolicyContext, ParticipantContextPolicyContext {

    @PolicyScope
    public static final String POLICY_MONITOR_SCOPE = "policy.monitor";

    private final Instant now;
    private final ContractAgreement contractAgreement;
    private ParticipantAgent participantAgent;

    public PolicyMonitorContext(Instant now, ContractAgreement contractAgreement) {
        this.now = now;
        this.contractAgreement = contractAgreement;
    }

    @Override
    public Instant now() {
        return now;
    }

    @Override
    public ContractAgreement contractAgreement() {
        return contractAgreement;
    }

    @Override
    public ParticipantAgent participantAgent() {
        if (participantAgent == null && contractAgreement != null) {
            var claims = contractAgreement.getClaims() == null ? Map.<String, Object>of() : contractAgreement.getClaims();
            participantAgent = new ParticipantAgent(contractAgreement.getConsumerId(), claims, Map.of());
        }
        return participantAgent;
    }

    @Override
    public String participantContextId() {
        return contractAgreement == null ? null : contractAgreement.getParticipantContextId();
    }

    @Override
    public String scope() {
        return POLICY_MONITOR_SCOPE;
    }
}
