/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.core;

import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.function.Function;

import static org.eclipse.edc.iam.decentralizedclaims.core.DcpCoreExtension.DEPRECATED_ISSUER_ID_KEY;
import static org.eclipse.edc.iam.decentralizedclaims.core.DcpCoreExtension.PARTICIPANT_DID;

/**
 * Provide configured DID.
 */
public class DidConfigProvider implements Function<String, String> {

    public static final String PARTICIPANT_ID = "edc.participant.id";

    private final ParticipantContextConfig config;
    private final Monitor monitor;

    public DidConfigProvider(ParticipantContextConfig config, Monitor monitor) {
        this.config = config;
        this.monitor = monitor;
    }

    @Override
    public String apply(String participantContextId) {
        var participantId = config.getString(participantContextId, PARTICIPANT_ID, null);
        if (participantId != null) {
            return participantId;
        }

        var participantDid = config.getString(participantContextId, PARTICIPANT_DID, null);
        if (participantDid != null) {
            return participantDid;
        }

        var participantDidDeprecated = config.getString(participantContextId, DEPRECATED_ISSUER_ID_KEY, null);
        if (participantDidDeprecated != null) {
            monitor.warning("Setting %s has been deprecated in favor of %s (or %s if the value configured is already the participant DID), please adapt your configuration"
                    .formatted(DEPRECATED_ISSUER_ID_KEY, PARTICIPANT_DID, PARTICIPANT_ID));
            return participantDidDeprecated;
        }

        return null;
    }
}
