/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.core.agent;

import org.eclipse.edc.participant.spi.ParticipantIdMapper;

/**
 * No-op implementation of the mapper, the ID is already its IRI representation.
 */
public class NoOpParticipantIdMapper implements ParticipantIdMapper {

    @Override
    public String toIri(String participantId) {
        return participantId;
    }

    @Override
    public String fromIri(String iriParticipantId) {
        return iriParticipantId;
    }
}
