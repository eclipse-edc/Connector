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

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Streams content into an OK HTTP buffered sink.
 */
public class StreamingRequestBody extends RequestBody {
    private final Supplier<InputStream> bodySupplier;
    private final String contentType;

    public StreamingRequestBody(Supplier<InputStream> contentSupplier, String contentType) {
        this.bodySupplier = contentSupplier;
        this.contentType = contentType;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        try (var os = sink.outputStream(); var is = bodySupplier.get()) {
            is.transferTo(os);
        }
    }
}
