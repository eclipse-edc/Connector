/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Contributors to the Eclipse Foundation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.catalog.spi;

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.policy.model.Policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ResolvedContractDefinitions(List<ContractDefinition> contractDefinitions, Map<String, Policy> policies) {

    public ResolvedContractDefinitions(List<ContractDefinition> contractDefinitions) {
        this(contractDefinitions, new HashMap<>());
    }
}
