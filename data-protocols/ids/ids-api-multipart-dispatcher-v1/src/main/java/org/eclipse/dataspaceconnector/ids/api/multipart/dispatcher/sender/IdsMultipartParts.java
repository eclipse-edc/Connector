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

    private final String header;

    @Nullable
    private final String payload;

    IdsMultipartParts(@NotNull String header, @Nullable String payload) {
        this.header = header;
        this.payload = payload;
    }

    @NotNull
    public String getHeader() {
        return header;
    }

    @Nullable
    public String getPayload() {
        return payload;
    }

    public static class Builder {
        private String header;

        @Nullable
        private String payload;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder header(String header) {
            this.header = header;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public IdsMultipartParts build() {
            Objects.requireNonNull(header, "header");
            return new IdsMultipartParts(header, payload);
        }
    }

}
