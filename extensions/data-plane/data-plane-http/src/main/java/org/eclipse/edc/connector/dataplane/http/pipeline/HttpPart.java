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

package org.eclipse.edc.connector.dataplane.http.pipeline;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;

import java.io.InputStream;

public record HttpPart(String name, InputStream content, String mediaType) implements DataSource.Part {

    @Override
    public long size() {
        return SIZE_UNKNOWN;
    }

    @Override
    public InputStream openStream() {
        return content;
    }

    @Override
    public String mediaType() {
        return mediaType;
    }
}
