/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.spi.contract.offer;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

/**
 * The result of searching for a specific contract definition.
 */
public class ContractDefinitionResult {
    public static final ContractDefinitionResult INVALID = new ContractDefinitionResult();

    private ContractDefinition definition;

    public ContractDefinitionResult(ContractDefinition definition) {
        this.definition = definition;
    }

    /**
     * Returns true if the search was invalid (e.g. no definition was found) or the requesting agent does not have access to the associated definition.
     */
    public boolean invalid() {
        return definition == null;
    }

    /**
     * Returns the contract definition. If the request was invalid, the definition will be null.
     */
    public ContractDefinition getDefinition() {
        return definition;
    }

    private ContractDefinitionResult() {
    }

}
