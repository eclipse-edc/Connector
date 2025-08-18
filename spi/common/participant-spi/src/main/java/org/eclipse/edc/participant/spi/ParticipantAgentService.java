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

package org.eclipse.edc.participant.spi;

import org.eclipse.edc.spi.iam.ClaimToken;

/**
 * Creates a {@link ParticipantAgent} from a claim token obtained from the requesting system.
 */
public interface ParticipantAgentService {

    /**
     * Creates a participant agent.
     *
     * @param token the token holding claims about the participant
     * @param participantId id of the participant
     * @return the ParticipantAgent
     */
    ParticipantAgent createFor(ClaimToken token, String participantId);

    /**
     * Registers an extension that can contribute attributes during the creation of a participant agent.
     */
    void register(ParticipantAgentServiceExtension extension);
}
