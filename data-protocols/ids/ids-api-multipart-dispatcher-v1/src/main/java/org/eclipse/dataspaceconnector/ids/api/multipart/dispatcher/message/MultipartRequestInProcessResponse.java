/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.fraunhofer.iais.eis.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

//TODO define return type

/**
 * Models a multipart response for an IDS RequestInProcessMessage. As the EDC communicates mostly
 * asynchronously, this message is the expected response for most request messages.
 */
public class MultipartRequestInProcessResponse implements MultipartResponse<String> {

    private final Message header;

    @Nullable
    private final String payload;

    private MultipartRequestInProcessResponse(@NotNull Message header, @Nullable String payload) {
        this.header = header;
        this.payload = payload;
    }

    @Override
    public @NotNull Message getHeader() {
        return header;
    }

    @Override
    public @Nullable String getPayload() {
        return payload;
    }

    public static class Builder {
        private Message header;
        private String payload;

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder header(Message header) {
            this.header = header;
            return this;
        }

        public Builder payload(@Nullable String payload) {
            this.payload = payload;
            return this;
        }

        public MultipartRequestInProcessResponse build() {
            Objects.requireNonNull(header, "header");
            return new MultipartRequestInProcessResponse(header, payload);
        }
    }

}
