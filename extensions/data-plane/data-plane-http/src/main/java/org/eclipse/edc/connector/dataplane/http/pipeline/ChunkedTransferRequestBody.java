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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.http.pipeline;

import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Streams content into an OK HTTP buffered sink in chunks.
 * <p>
 * Due to OkHttp implementation an extra header will be created (no-overridable) Transfer-Encoding with value chunked
 *
 * @see <a href="https://github.com/square/okhttp/blob/master/docs/features/calls.md">OkHttp Dcoumentation</a>
 */
public class ChunkedTransferRequestBody extends AbstractTransferRequestBody {

    private final Supplier<InputStream> bodySupplier;

    public ChunkedTransferRequestBody(Supplier<InputStream> bodySupplier, String contentType) {
        super(contentType);
        this.bodySupplier = bodySupplier;
    }

    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        try (var os = sink.outputStream(); var is = bodySupplier.get()) {
            is.transferTo(os);
        }
    }
}
