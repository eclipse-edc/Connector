/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.spi;


import org.eclipse.edc.participant.spi.ParticipantAgentServiceExtension;

/**
 * Marker class for a {@link ParticipantAgentServiceExtension} that will be registered for Identity And Trust
 * module.
 */
public interface DcpParticipantAgentServiceExtension extends ParticipantAgentServiceExtension {
}
