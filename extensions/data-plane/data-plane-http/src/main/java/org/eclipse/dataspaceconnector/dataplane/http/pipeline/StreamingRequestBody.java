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
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;

import java.io.IOException;

/**
 * Streams content into an OK HTTP buffered sink.
 */
public class StreamingRequestBody extends RequestBody {
    private static final String OCTET_STREAM = "application/octet-stream";

    private final DataSource.Part part;

    public StreamingRequestBody(DataSource.Part part) {
        this.part = part;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(OCTET_STREAM);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        part.openStream().transferTo(sink.outputStream());
    }

}
