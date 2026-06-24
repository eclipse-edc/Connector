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

package org.eclipse.edc.connector.dataplane.spi.resolver;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Resolves an access token into a {@link DataAddress}.
 */
@FunctionalInterface
public interface DataAddressResolver {

    /**
     * Resolve token to a {@link DataAddress}.
     *
     * @param token Access token.
     * @return Data address.
     */
    Result<DataAddress> resolve(String token);
}
