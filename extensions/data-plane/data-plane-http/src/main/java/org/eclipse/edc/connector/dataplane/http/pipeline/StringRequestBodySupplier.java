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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Supplier for string request body.
 */
public class StringRequestBodySupplier implements Supplier<InputStream> {

    private final String body;

    public StringRequestBodySupplier(String requestBody) {
        Objects.requireNonNull(requestBody);
        this.body = requestBody;
    }

    @Override
    public InputStream get() {
        return new ByteArrayInputStream(body.getBytes());
    }
}
