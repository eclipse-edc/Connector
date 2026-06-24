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

@FunctionalInterface
public interface CelParticipantAgentClaimMapper {

    /**
     * Maps a claim to a CEL claim representation.
     *
     * @param agent the participant agent for which to map the claim
     * @return the mapped CEL claim
     */
    CelClaim mapClaim(ParticipantAgent agent);
}
