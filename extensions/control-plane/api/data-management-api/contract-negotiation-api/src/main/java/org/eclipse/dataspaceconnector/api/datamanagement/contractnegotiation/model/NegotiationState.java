/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;

/**
 * Wrapper for {@link ContractNegotiationStates} formatted as String. Used to format a simple string as JSON.
 */
public class NegotiationState {
    private final String state;

    public NegotiationState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}
