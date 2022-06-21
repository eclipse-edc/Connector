/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.contract.offer;

import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.policy.PolicyScope;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Returns {@link ContractDefinition} for a given participant agent.
 *
 * A runtime extension may implement custom logic to determine which contract definitions are returned.
 */
public interface ContractDefinitionService {

    @PolicyScope
    String NEGOTIATION_SCOPE = "contract.negotiation";

    /**
     * Returns the definitions for the given participant agent.
     */
    @NotNull
    Stream<ContractDefinition> definitionsFor(ParticipantAgent agent);

    /**
     * Returns a contract definition for the agent associated with the given contract definition id.
     * If the definition does not exist or the agent is not authorized, the result will indicate the request is invalid.
     */
    @Nullable
    ContractDefinition definitionFor(ParticipantAgent agent, String definitionId);
}
