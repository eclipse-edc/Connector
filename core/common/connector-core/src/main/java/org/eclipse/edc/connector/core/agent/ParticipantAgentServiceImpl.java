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
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *
 */

package org.eclipse.edc.connector.core.agent;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentService;
import org.eclipse.edc.participant.spi.ParticipantAgentServiceExtension;
import org.eclipse.edc.spi.iam.ClaimToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.eclipse.edc.participant.spi.ParticipantAgent.PARTICIPANT_IDENTITY;

/**
 * Default implementation.
 */
public class ParticipantAgentServiceImpl implements ParticipantAgentService {

    private final List<ParticipantAgentServiceExtension> extensions = new ArrayList<>();

    public ParticipantAgentServiceImpl() { }

    @Override
    public ParticipantAgent createFor(ClaimToken token, String participantId) {
        var attributes = new HashMap<String, String>();
        
        extensions.stream().map(extension -> extension.attributesFor(token)).forEach(attributes::putAll);

        attributes.put(PARTICIPANT_IDENTITY, participantId);
        return new ParticipantAgent(token.getClaims(), attributes);
    }

    @Override
    public void register(ParticipantAgentServiceExtension extension) {
        extensions.add(extension);
    }
}
