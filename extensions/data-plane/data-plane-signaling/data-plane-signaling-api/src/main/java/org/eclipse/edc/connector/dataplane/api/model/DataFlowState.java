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

package org.eclipse.edc.connector.dataplane.api.model;

import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Wrapper for {@link DataFlowStates} formatted as String. Used to format a simple string as JSON.
 */
public record DataFlowState(String state) {
    public static final String DATA_FLOW_STATE_SIMPLE_TYPE = "DataFlowState";
    public static final String DATA_FLOW_STATE_TYPE = EDC_NAMESPACE + DATA_FLOW_STATE_SIMPLE_TYPE;
    public static final String DATA_FLOW_STATE_STATE = EDC_NAMESPACE + "state";

}
