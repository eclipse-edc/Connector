/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.http.client;

import okhttp3.ResponseBody;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Reads HTTP response bodies with a size limit.
 */
public final class ResponseBodies {

    private ResponseBodies() {
    }

    /**
     * Reads the body as an UTF-8 string, failing when it is larger than the given number of bytes. At most
     * {@code maxBytes + 1} bytes are read from the response.
     *
     * @param body     the response body.
     * @param maxBytes the maximum number of bytes.
     * @return the body content, or a failure if it is larger than {@code maxBytes}.
     * @throws IOException if the body cannot be read.
     */
    public static Result<String> readUtf8(ResponseBody body, long maxBytes) throws IOException {
        var source = body.source();
        if (source.request(maxBytes + 1)) {
            return failure("The response body exceeds the maximum size of %d bytes".formatted(maxBytes));
        }
        return success(source.getBuffer().readString(StandardCharsets.UTF_8));
    }
}
