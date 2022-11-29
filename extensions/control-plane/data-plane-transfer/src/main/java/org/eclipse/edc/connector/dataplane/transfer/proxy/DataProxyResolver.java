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

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

@FunctionalInterface
public interface DataProxyResolver {
    /**
     * Resolve the public Api URL of the Data Plane that will by used as proxy for querying the data.
     *
     * @param source Source data address.
     * @return URL of the Data Plane to be used as proxy.
     */
    Result<String> resolveProxyUrl(DataAddress source);
}
