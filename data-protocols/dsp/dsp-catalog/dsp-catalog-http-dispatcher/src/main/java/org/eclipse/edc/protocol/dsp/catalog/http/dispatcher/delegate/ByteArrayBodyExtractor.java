/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.http.dispatcher.delegate;

import okhttp3.ResponseBody;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.response.DspHttpResponseBodyExtractor;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;

/**
 * Extract the body as a byte[]
 */
public class ByteArrayBodyExtractor implements DspHttpResponseBodyExtractor<byte[]> {
    @Override
    public byte[] extractBody(ResponseBody responseBody, String protocol) {
        try {
            if (responseBody == null) {
                return null;
            }
            return responseBody.bytes();

        } catch (IOException e) {
            throw new EdcException("Failed to read response body", e);
        }
    }
}
