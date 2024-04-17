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

package org.eclipse.edc.connector.controlplane.transfer.flow;

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.FlowTypeExtractor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;

import java.util.Optional;

public class FlowTypeExtractorImpl implements FlowTypeExtractor {
    @Override
    public StatusResult<FlowType> extract(String transferType) {
        return Optional.ofNullable(transferType)
                .map(type -> type.split("-"))
                .filter(tokens -> tokens.length == 2)
                .map(tokens -> parseFlowType(tokens[1]))
                .orElse(StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to extract flow type from transferType %s".formatted(transferType)));
    }

    private StatusResult<FlowType> parseFlowType(String flowType) {
        try {
            return StatusResult.success(FlowType.valueOf(flowType));
        } catch (Exception e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Unknown flow type %s".formatted(flowType));
        }
    }
}
