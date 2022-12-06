/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.message;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents an IDS multipart response. Contains the IDS message header and an optional payload.
 */
public class MultipartResponse {

    private final Message header;
    private final Object payload;

    private MultipartResponse(@NotNull Message header, @Nullable Object payload) {
        this.header = header;
        this.payload = payload;
    }

    @NotNull
    public Message getHeader() {
        return header;
    }

    @Nullable
    public Object getPayload() {
        return payload;
    }

    /**
     * Sets the security token on the header created by the getToken function from the header itself.
     *
     * @param getToken a function that creates the security token giving the header
     */
    public void setSecurityToken(Function<Message, DynamicAttributeToken> getToken) {
        getHeader().setSecurityToken(getToken.apply(getHeader()));
    }

    public static class Builder {

        private Message header;
        private Object payload;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder header(@Nullable Message header) {
            this.header = header;
            return this;
        }

        public Builder payload(@Nullable Object payload) {
            this.payload = payload;
            return this;
        }

        public MultipartResponse build() {
            Objects.requireNonNull(header, "Multipart response header is null.");
            return new MultipartResponse(header, payload);
        }
    }
}
