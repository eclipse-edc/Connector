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

package org.eclipse.edc.connector.controlplane.services.spi.protocol;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Token validator to be used in protocol layer for verifying the token according the
 * input policy and policy scope
 */
@ExtensionPoint
public interface ProtocolTokenValidator {

    /**
     * Verify the {@link TokenRepresentation}
     *
     * @param tokenRepresentation   The token
     * @param policyContextProvider The policy scope
     * @return Returns the extracted {@link ParticipantAgent} if successful, failure otherwise
     */
    default ServiceResult<ParticipantAgent> verify(TokenRepresentation tokenRepresentation, RequestPolicyContext.Provider policyContextProvider) {
        return verify(tokenRepresentation, policyContextProvider, Policy.Builder.newInstance().build(), null);
    }
    
    /**
     * Verify the {@link TokenRepresentation}
     *
     * @param tokenRepresentation   The token
     * @param policyContextProvider The policy scope
     * @param message               The {@link RemoteMessage}
     * @return Returns the extracted {@link ParticipantAgent} if successful, failure otherwise
     */
    default ServiceResult<ParticipantAgent> verify(TokenRepresentation tokenRepresentation, RequestPolicyContext.Provider policyContextProvider, RemoteMessage message) {
        return verify(tokenRepresentation, policyContextProvider, Policy.Builder.newInstance().build(), message);
    }

    /**
     * Verify the {@link TokenRepresentation} in the context of a policy
     *
     * @param tokenRepresentation   The token
     * @param policyContextProvider The policy scope provider
     * @param policy                The policy
     * @param message               The {@link RemoteMessage}
     * @return Returns the extracted {@link ParticipantAgent} if successful, failure otherwise
     */
    ServiceResult<ParticipantAgent> verify(TokenRepresentation tokenRepresentation, RequestPolicyContext.Provider policyContextProvider, Policy policy, RemoteMessage message);
}
