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

package org.eclipse.edc.connector.core.base.agent;

import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.agent.ParticipantAgentServiceExtension;
import org.eclipse.edc.spi.iam.ClaimToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.spi.agent.ParticipantAgent.PARTICIPANT_IDENTITY;

/**
 * Default implementation.
 */
public class ParticipantAgentServiceImpl implements ParticipantAgentService {
    private final String identityClaimKey;
    private final List<ParticipantAgentServiceExtension> extensions = new ArrayList<>();

    public ParticipantAgentServiceImpl() {
        identityClaimKey = DEFAULT_IDENTITY_CLAIM_KEY;
    }

    public ParticipantAgentServiceImpl(String key) {
        requireNonNull(key, "key");
        this.identityClaimKey = key;
    }

    @Override
    public ParticipantAgent createFor(ClaimToken token) {
        var attributes = new HashMap<String, String>();
        extensions.stream().map(extension -> extension.attributesFor(token)).forEach(attributes::putAll);
        if (!attributes.containsKey(PARTICIPANT_IDENTITY)) {
            var claim = token.getClaim(identityClaimKey);
            if (claim != null) {
                attributes.put(PARTICIPANT_IDENTITY, claim.toString());
            }
        }
        return new ParticipantAgent(token.getClaims(), attributes);
    }

    @Override
    public void register(ParticipantAgentServiceExtension extension) {
        extensions.add(extension);
    }
}
