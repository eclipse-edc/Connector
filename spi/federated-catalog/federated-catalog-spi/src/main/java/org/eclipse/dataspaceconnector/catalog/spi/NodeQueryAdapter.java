/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.spi;


import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Takes an {@link UpdateRequest}, sends it to the intended FCN endpoint using a particular application protocol (e.g. IDS) to get that
 * endpoint's catalog.
 * <p>
 * For example, an {@code IdsProtocolAdapter} would perform an IDS Description Request to whatever URL is contained in the {@code UpdateRequest}
 * and return the response to that.
 */
public interface NodeQueryAdapter {
    CompletableFuture<UpdateResponse> sendRequest(UpdateRequest request);
}
