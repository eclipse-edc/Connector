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

package org.eclipse.edc.connector.controlplane.transfer.spi.flow;

import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;

/**
 * Extract the {@link FlowType} from the transfer type
 */
@FunctionalInterface
public interface FlowTypeExtractor {

    /**
     * Return the {@link FlowType} associated to the transfer type.
     *
     * @param transferType the transfer type.
     * @return the {@link FlowType}, failure if the operation failed.
     */
    StatusResult<FlowType> extract(String transferType);
}
