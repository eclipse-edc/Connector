/*
 *  Copyright (c) 2020, 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */


package org.eclipse.dataspaceconnector.ids.api.multipart.client.message;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Objects;

public class IdsMultipartParts {

    private InputStream header;

    @Nullable
    private InputStream payload;

    public InputStream getHeader() {
        return header;
    }

    public @Nullable InputStream getPayload() {
        return payload;
    }

    public static class Builder {
        private final IdsMultipartParts parts;

        private Builder() {
            this.parts = new IdsMultipartParts();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder header(InputStream header) {
            this.parts.header = header;
            return this;
        }

        public Builder payload(InputStream payload) {
            this.parts.payload = payload;
            return this;
        }

        public IdsMultipartParts build() {
            Objects.requireNonNull(parts.header);
            return parts;
        }
    }

}
