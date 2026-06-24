/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.policy.cel.function.context;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.util.List;

@ExtensionPoint
public interface CelParticipantAgentClaimMapperRegistry {

    /**
     * Registers a claim mapper for participant agents.
     *
     * @param claimMapper the claim mapper to register
     */
    void registerClaimMapper(CelParticipantAgentClaimMapper claimMapper);

    /**
     * Maps a participant agent to a CEL claim using the registered claim mappers.
     *
     * @param agent the participant agent to map
     * @return the mapped CEL claim, or null if no mapper could handle the agent
     */
    List<CelClaim> mapClaim(ParticipantAgent agent);
}
