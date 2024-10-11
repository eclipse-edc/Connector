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

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyScope;

import java.time.Instant;

/**
 * Policy Context for "policy-monitor" scope
 */
public class PolicyMonitorContext extends PolicyContextImpl {

    @PolicyScope
    public static final String POLICY_MONITOR_SCOPE = "policy.monitor";

    private final Instant now;
    private final ContractAgreement contractAgreement;

    public PolicyMonitorContext(Instant now, ContractAgreement contractAgreement) {
        this.now = now;
        this.contractAgreement = contractAgreement;
    }

    public Instant now() {
        return now;
    }

    public ContractAgreement contractAgreement() {
        return contractAgreement;
    }

    @Override
    public String scope() {
        return POLICY_MONITOR_SCOPE;
    }
}
