/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Wrapper for {@link ContractNegotiationStates} formatted as String. Used to format a simple string as JSON.
 */
public record NegotiationState(String state) {

    public static final String NEGOTIATION_STATE_TYPE = EDC_NAMESPACE + "NegotiationState";
    public static final String NEGOTIATION_STATE_STATE = EDC_NAMESPACE + "state";

}
