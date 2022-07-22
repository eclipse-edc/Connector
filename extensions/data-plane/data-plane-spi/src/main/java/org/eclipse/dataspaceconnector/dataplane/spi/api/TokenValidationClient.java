/*
 *  Copyright (c) 2022 Amadeus
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

package org.eclipse.dataspaceconnector.dataplane.spi.api;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

/**
 * Client used by the data plane to hit the validation server (that can be hosted by the Control Plane for example).
 * The validation server will assert the validity of the token following a set of rules, and if successful, it will
 * return the decrypted data address contained in the input token.
 */
@FunctionalInterface
public interface TokenValidationClient {

    /**
     * Hits the token validation endpoint to verify if the provided token is valid.
     *
     * @param token Token received in input of the data plane.
     * @return Decrypted {@link DataAddress} contained in the input claim token.
     */
    Result<DataAddress> call(String token);
}
