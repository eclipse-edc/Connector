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

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

/**
 * Uses Data Plane as a proxy for querying data from the data source.
 */
@FunctionalInterface
public interface DataPlaneTransferProxyCreator {
    Result<EndpointDataReference> createProxy(@NotNull DataPlaneTransferProxyCreationRequest request);
}
