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

package org.eclipse.dataspaceconnector.spi.agent;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;

/**
 * Creates a {@link ParticipantAgent} from a claim token obtained from the requesting system.
 */
public interface ParticipantAgentService {

    /**
     * Creates a participant agent.
     */
    ParticipantAgent createFor(ClaimToken token);

    /**
     * Registers an extension that can contribute attributes during the creation of a participant agent.
     */
    void register(ParticipantAgentServiceExtension extension);
}
