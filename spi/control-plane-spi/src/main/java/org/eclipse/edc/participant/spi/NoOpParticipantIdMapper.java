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

package org.eclipse.edc.participant.spi;

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
