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

package org.eclipse.edc.connector.contract.spi.offer;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Returns {@link ContractDefinition} for a given participant agent.
 * <p>
 * A runtime extension may implement custom logic to determine which contract definitions are returned.
 */
@ExtensionPoint
public interface ContractDefinitionResolver {

    @PolicyScope
    String CATALOGING_SCOPE = "catalog";

    /**
     * Returns the definitions for the given participant agent.
     */
    @NotNull
    Stream<ContractDefinition> definitionsFor(ParticipantAgent agent);

    /**
     * Returns a contract definition for the agent associated with the given contract definition id. If the definition
     * does not exist or the agent is not authorized, the result will indicate the request is invalid.
     */
    @Nullable
    ContractDefinition definitionFor(ParticipantAgent agent, String definitionId);
}
