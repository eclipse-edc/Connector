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

package org.eclipse.edc.connector.api.management.contractnegotiation.model;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Wrapper for {@link ContractNegotiationStates} formatted as String. Used to format a simple string as JSON.
 */
public record NegotiationState(String state) {

    public static final String NEGOTIATION_STATE_TYPE = EDC_NAMESPACE + "NegotiationState";
    public static final String NEGOTIATION_STATE_STATE = EDC_NAMESPACE + "state";

}
