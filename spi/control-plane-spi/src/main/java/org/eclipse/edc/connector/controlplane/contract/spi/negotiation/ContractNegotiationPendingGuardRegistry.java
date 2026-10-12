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

package org.eclipse.edc.connector.controlplane.contract.spi.negotiation;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Registry of {@link ContractNegotiationPendingGuard}s. It matches a negotiation when any of the registered guards
 * matches it, so that multiple guards can be active at the same time.
 */
@ExtensionPoint
public interface ContractNegotiationPendingGuardRegistry extends ContractNegotiationPendingGuard {

    /**
     * Registers a guard.
     *
     * @param guard the guard.
     */
    void register(ContractNegotiationPendingGuard guard);

}
