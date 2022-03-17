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
package org.eclipse.dataspaceconnector.core.base.agent;

import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgentServiceExtension;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Default implementation.
 */
public class ParticipantAgentServiceImpl implements ParticipantAgentService {
    private List<ParticipantAgentServiceExtension> extensions = new ArrayList<>();

    @Override
    public ParticipantAgent createFor(ClaimToken token) {
        var attributes = extensions.stream().flatMap(extension -> extension.attributesFor(token).entrySet().stream())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new ParticipantAgent(token.getClaims(), attributes);
    }

    @Override
    public void register(ParticipantAgentServiceExtension extension) {
        extensions.add(extension);
    }
}
