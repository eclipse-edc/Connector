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

package org.eclipse.edc.connector.transfer.spi.flow;

import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.Map;

/**
 * Extension point allows additional properties to be included in a {@link DataFlowStartMessage}
 */
@FunctionalInterface
@ExtensionPoint
public interface DataFlowPropertiesProvider {

    StatusResult<Map<String, String>> propertiesFor(TransferProcess transferProcess, Policy policy);
    
}
