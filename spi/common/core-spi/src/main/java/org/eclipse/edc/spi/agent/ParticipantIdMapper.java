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

package org.eclipse.edc.spi.agent;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Map participant ID from/to the IRI representation
 */
@ExtensionPoint
public interface ParticipantIdMapper {

    /**
     * Return IRI representation of the participant ID.
     *
     * @return IRI representation.
     */
    String toIri(String participantId);

    /**
     * Extract participant ID from the IRI representation.
     *
     * @return participant ID.
     */
    String fromIri(String iriParticipantId);

}
