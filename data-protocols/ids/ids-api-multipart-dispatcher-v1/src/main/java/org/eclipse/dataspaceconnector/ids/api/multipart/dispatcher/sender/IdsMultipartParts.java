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

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Objects;

/**
 * Container object for the header and payload part of an IDS multipart response as
 * {@link InputStream}s, so that they can easily be parsed to the correct form.
 */
class IdsMultipartParts {

    private final InputStream header;

    @Nullable
    private final InputStream payload;

    IdsMultipartParts(@NotNull InputStream header, @Nullable InputStream payload) {
        this.header = header;
        this.payload = payload;
    }

    @NotNull
    public InputStream getHeader() {
        return header;
    }

    @Nullable
    public InputStream getPayload() {
        return payload;
    }

    public static class Builder {
        private InputStream header;

        @Nullable
        private InputStream payload;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder header(InputStream header) {
            this.header = header;
            return this;
        }

        public Builder payload(InputStream payload) {
            this.payload = payload;
            return this;
        }

        public IdsMultipartParts build() {
            Objects.requireNonNull(header, "header");
            return new IdsMultipartParts(header, payload);
        }
    }

}
