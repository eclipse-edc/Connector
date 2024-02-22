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

package org.eclipse.edc.spi.types.domain.transfer;

import org.eclipse.edc.spi.types.domain.DataAddress;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * A response message from the data plane upon receiving a {@link DataFlowStartMessage}
 */
public record DataFlowResponseMessage(DataAddress dataAddress) {

    public static final String DATA_FLOW_RESPONSE_MESSAGE_SIMPLE_TYPE = "DataFlowResponseMessage";
    public static final String DATA_FLOW_RESPONSE_MESSAGE_TYPE = EDC_NAMESPACE + DATA_FLOW_RESPONSE_MESSAGE_SIMPLE_TYPE;
    public static final String DATA_FLOW_RESPONSE_MESSAGE_DATA_ADDRESS = EDC_NAMESPACE + "dataAddress";

}
