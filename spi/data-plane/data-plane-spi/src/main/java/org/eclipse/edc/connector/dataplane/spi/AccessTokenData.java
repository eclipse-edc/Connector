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

package org.eclipse.edc.connector.dataplane.spi;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;

/**
 * Container object for a {@link ClaimToken} and a {@link DataAddress} that the data plane uses to keep track of currently
 * all access tokens that are currently valid.
 *
 * @param id                   The correlation ID of the EDR, that is authorized for this data resources. The token, that is stored inside the
 *                             EDR must carry this information. For JWTs this would be the "jti" claim.
 * @param claimToken           The representation of the EDR
 * @param dataAddress          The data resource (= source address) for which the token is authorized
 * @param additionalProperties (optional) a list of additional properties that should be persisted with the AccessTokenData, for example refresh tokens, etc.
 */
public record AccessTokenData(String id, ClaimToken claimToken, DataAddress dataAddress,
                              Map<String, Object> additionalProperties) {

    public AccessTokenData(String id, ClaimToken claimToken, DataAddress dataAddress) {
        this(id, claimToken, dataAddress, Map.of());
    }
}
