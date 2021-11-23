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

package org.eclipse.dataspaceconnector.ids.api.multipart.client.message;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ResponseMessage;
import org.jetbrains.annotations.Nullable;

//TODO define return type
public class MultipartRequestInProcessResponse implements MultipartResponse<String> {

    Message header;

    @Nullable
    private String payload;

    private MultipartRequestInProcessResponse() { }

    @Override
    public Message getHeader() {
        return header;
    }

    @Override
    public @Nullable String getPayload() {
        return payload;
    }

    public static class Builder {
        private final MultipartRequestInProcessResponse requestInProcessResponse;

        private Builder() {
            this.requestInProcessResponse = new MultipartRequestInProcessResponse();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder header(ResponseMessage header) {
            this.requestInProcessResponse.header = header;
            return this;
        }

        public Builder payload(@Nullable String payload) {
            this.requestInProcessResponse.payload = payload;
            return this;
        }

        public MultipartRequestInProcessResponse build() {
            Objects.requireNonNull(requestInProcessResponse.header, "header");
            return requestInProcessResponse;
        }
    }

}
