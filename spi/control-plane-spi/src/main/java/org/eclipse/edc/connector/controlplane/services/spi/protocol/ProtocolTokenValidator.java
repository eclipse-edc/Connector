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
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *
 */

package org.eclipse.edc.connector.controlplane.services.spi.protocol;

import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Token validator to be used in protocol layer for verifying the token according the
 * input policy and the request message
 */
@ExtensionPoint
public interface ProtocolTokenValidator {

    /**
     * Verify the {@link TokenRepresentation}
     *
     * @param tokenRepresentation The token
     * @param message             The {@link ProtocolRemoteMessage}
     * @return Returns the extracted {@link ParticipantAgent} if successful, failure otherwise
     */
    default ServiceResult<ParticipantAgent> verify(ParticipantContext participantContext, TokenRepresentation tokenRepresentation, ProtocolRemoteMessage message) {
        return verify(participantContext, tokenRepresentation, Policy.Builder.newInstance().build(), message);
    }


    /**
     * Verify the {@link TokenRepresentation} in the context of a policy
     *
     * @param tokenRepresentation The token
     * @param policy              The policy
     * @param message             The {@link ProtocolRemoteMessage}
     * @return Returns the extracted {@link ParticipantAgent} if successful, failure otherwise
     */
    ServiceResult<ParticipantAgent> verify(ParticipantContext participantContext, TokenRepresentation tokenRepresentation, Policy policy, ProtocolRemoteMessage message);
}
