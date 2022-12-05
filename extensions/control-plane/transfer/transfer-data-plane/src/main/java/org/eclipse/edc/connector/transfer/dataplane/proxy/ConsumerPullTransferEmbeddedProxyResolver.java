/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.proxy;

import org.eclipse.edc.connector.dataplane.spi.DataPlanePublicApiUrl;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

public class ConsumerPullTransferEmbeddedProxyResolver implements ConsumerPullTransferProxyResolver {


    private final DataPlanePublicApiUrl embeddedUrl;

    public ConsumerPullTransferEmbeddedProxyResolver(DataPlanePublicApiUrl embeddedUrl) {
        this.embeddedUrl = embeddedUrl;
    }

    @Override
    public Result<String> resolveProxyUrl(DataAddress source) {
        return Result.success(embeddedUrl.get().toString());
    }
}
