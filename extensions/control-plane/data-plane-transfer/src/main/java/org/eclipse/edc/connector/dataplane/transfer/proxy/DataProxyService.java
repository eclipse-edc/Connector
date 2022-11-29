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

package org.eclipse.edc.connector.dataplane.transfer.proxy;

import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Generates {@link org.eclipse.edc.spi.types.domain.edr.EndpointDataReference} targetting the Data Plane public API and
 * dispatch it to the consumer Control Plane.
 */
@FunctionalInterface
public interface DataProxyService {
    StatusResult<Void> createProxyReferenceAndDispatch(DataRequest dataRequest, DataAddress contentAddress);
}
