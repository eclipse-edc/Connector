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

import java.util.ArrayList;
import java.util.List;

public class CelParticipantAgentClaimMapperRegistryImpl implements CelParticipantAgentClaimMapperRegistry {

    protected List<CelParticipantAgentClaimMapper> claimMappers = new ArrayList<>();

    @Override
    public void registerClaimMapper(CelParticipantAgentClaimMapper claimMapper) {
        claimMappers.add(claimMapper);
    }

    @Override
    public List<CelClaim> mapClaim(ParticipantAgent agent) {
        return claimMappers.stream()
                .map(mapper -> mapper.mapClaim(agent))
                .toList();
    }
}
