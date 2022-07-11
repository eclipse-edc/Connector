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
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Streams content into an OK HTTP buffered sink.
 */
public class NonChunkedTransferRequestBody extends RequestBody {
    private Optional<byte[]> bytes;
    private final String contentType;

    public NonChunkedTransferRequestBody(Supplier<InputStream> contentSupplier, String contentType) {
        try {
            this.bytes = Optional.of(contentSupplier.get().readAllBytes());
        } catch (IOException e) {
            this.bytes = Optional.empty();
        }
        this.contentType = contentType;
    }

    @Override
    public long contentLength() {
        return bytes.map(value -> value.length).orElse(0);
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        if (!bytes.isPresent()) {
            return;
        }

        try (var os = sink.outputStream()) {
            os.write(bytes.get());
        }
    }
}

