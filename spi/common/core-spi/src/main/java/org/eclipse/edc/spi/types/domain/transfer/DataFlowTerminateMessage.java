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

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * A message for terminating an in progress transfer in the data plane
 */
public record DataFlowTerminateMessage(String reason) {

    public static final String DATA_FLOW_TERMINATE_MESSAGE_SIMPLE_TYPE = "DataFlowTerminateMessage";
    public static final String DATA_FLOW_TERMINATE_MESSAGE_TYPE = EDC_NAMESPACE + DATA_FLOW_TERMINATE_MESSAGE_SIMPLE_TYPE;
    public static final String DATA_FLOW_TERMINATE_MESSAGE_REASON = EDC_NAMESPACE + "reason";

}
