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
import org.eclipse.edc.policy.engine.spi.PolicyContext;

import java.time.Instant;

/**
 * Marker interface
 */
public interface AgreementPolicyContext extends PolicyContext {

    /**
     * The contract agreement.
     *
     * @return The contract agreement.
     */
    ContractAgreement contractAgreement();

    /**
     * Current timestamp representation.
     *
     * @return Current timestamp representation.
     */
    Instant now();

}
