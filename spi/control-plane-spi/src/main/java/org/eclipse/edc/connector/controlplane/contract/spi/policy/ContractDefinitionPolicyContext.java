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

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.jetbrains.annotations.Nullable;

/**
 * Policy context that carries the {@link ContractDefinition} the policy is evaluated for.
 */
public interface ContractDefinitionPolicyContext extends PolicyContext {

    /**
     * The contract definition.
     *
     * @return the contract definition, null if not available.
     */
    @Nullable
    ContractDefinition contractDefinition();

}
