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

package org.eclipse.edc.connector.spi.protocol;

import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Token validator to be used in protocol layer for verifying the token according the
 * input policy and policy scope
 */
@ExtensionPoint
public interface ProtocolTokenValidator {
    
    /**
     * Verify the {@link TokenRepresentation} in the context of a policy
     *
     * @param tokenRepresentation The token
     * @param policyScope         The policy scope
     * @param policy              The policy
     * @return Returns the extracted {@link ClaimToken} if successful, failure otherwise
     * @deprecated please use {@link #verify(TokenRepresentation, String, Policy)}
     */
    @Deprecated(since = "0.5.1")
    ServiceResult<ClaimToken> verifyToken(TokenRepresentation tokenRepresentation, String policyScope, Policy policy);

    /**
     * Verify the {@link TokenRepresentation} in the context of a policy
     *
     * @param tokenRepresentation The token
     * @param policyScope         The policy scope
     * @param policy              The policy
     * @return Returns the extracted {@link ParticipantAgent} if successful, failure otherwise
     */
    ServiceResult<ParticipantAgent> verify(TokenRepresentation tokenRepresentation, String policyScope, Policy policy);
}
