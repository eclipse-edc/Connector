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

package org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy;

import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for service building a bearer token used as input of Data Plane public API to retrievedata located at a specific address.
 */
@FunctionalInterface
public interface DataPlaneProxyTokenGenerator {
    /**
     * Generate a token wrapping a data source.
     *
     * @param dataAddress address of the actual data that will be hit by the data plane.
     * @param contractId  contract identifier.
     */
    Result<TokenRepresentation> generate(@NotNull DataAddress dataAddress, @NotNull String contractId);
}
