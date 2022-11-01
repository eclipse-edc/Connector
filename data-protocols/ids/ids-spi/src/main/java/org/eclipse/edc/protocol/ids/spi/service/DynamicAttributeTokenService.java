/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.ids.spi.service;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.result.Result;

import java.net.URI;

/**
 * Wrapper for the {@link IdentityService} that handles
 * {@link DynamicAttributeToken}s.
 */
@ExtensionPoint
public interface DynamicAttributeTokenService {

    /**
     * Obtains a client token as a DynamicAttributeToken.
     *
     * @param recipientAddress the recipient connector.
     * @return the token result.
     */
    Result<DynamicAttributeToken> obtainDynamicAttributeToken(String recipientAddress);

    /**
     * Verifies a received DynamicAttributeToken.
     *
     * @param token           the token to verify.
     * @param issuerConnector the connector that sent the token.
     * @param audience        the token audience.
     * @return the result of the validation.
     */
    Result<ClaimToken> verifyDynamicAttributeToken(DynamicAttributeToken token, URI issuerConnector, String audience);

}
