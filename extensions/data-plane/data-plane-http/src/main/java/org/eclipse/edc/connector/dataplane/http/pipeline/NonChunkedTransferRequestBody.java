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

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Writes content into an OK HTTP buffered sink.
 *
 * The extra Transfer-Encoding is not created because the Content-Length is provided upfront.
 * Note that means that the all content is loaded into memory, so this method can be used for small files (up to 50MB) for e.g.
 *
 * @see <a href="https://github.com/square/okhttp/blob/master/docs/features/calls.md">OkHttp Dcoumentation</a>
 */
public class NonChunkedTransferRequestBody extends RequestBody {
    private byte[] bytes;
    private final String contentType;

    public NonChunkedTransferRequestBody(Supplier<InputStream> contentSupplier, String contentType) {
        try {
            this.bytes = contentSupplier.get().readAllBytes();
        } catch (IOException e) {
            //do nothing
        }
        this.contentType = contentType;
    }

    @Override
    public long contentLength() {
        return bytes == null ? 0 : bytes.length;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        if (bytes == null) {
            return;
        }

        try (var os = sink.outputStream()) {
            os.write(bytes);
        }
    }
}

