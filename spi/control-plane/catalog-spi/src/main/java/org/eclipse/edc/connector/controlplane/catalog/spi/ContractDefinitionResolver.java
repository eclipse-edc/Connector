/*
 *  Copyright (c) 2024 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.catalog.spi;

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Returns {@link ContractDefinition} for a given participant agent.
 * <p>
 * A runtime extension may implement custom logic to determine which contract definitions are returned.
 */
@ExtensionPoint
public interface ContractDefinitionResolver {

    /**
     * Returns definitions for the given participant given, plus the access policy objects related.
     *
     * @param agent the participant agent.
     * @return resolved contract definitions.
     */
    ResolvedContractDefinitions resolveFor(ParticipantAgent agent);
}
