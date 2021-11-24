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

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ModelClass;
import de.fraunhofer.iais.eis.ResponseMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Models a multipart response for an IDS DescriptionResponseMessage. This message is the expected
 * response for MetadataRequests/IDS DescriptionRequestMessages and contains Infomodel metadata
 * in the payload. Therefore {@link ModelClass}, which is the super class for all Infomodel classes,
 * is set as the payload type.
 */
public class MultipartDescriptionResponse implements MultipartResponse<ModelClass> {

    private final ResponseMessage header;
    private final ModelClass payload;

    private MultipartDescriptionResponse(@NotNull ResponseMessage header, @NotNull ModelClass payload) {
        this.header = header;
        this.payload = payload;
    }

    @Override
    public @NotNull Message getHeader() {
        return header;
    }

    @Override
    public @NotNull ModelClass getPayload() {
        return payload;
    }

    public static class Builder {
        private ResponseMessage header;
        private ModelClass payload;

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder header(ResponseMessage header) {
            this.header = header;
            return this;
        }

        public Builder payload(ModelClass payload) {
            this.payload = payload;
            return this;
        }

        public MultipartDescriptionResponse build() {
            Objects.requireNonNull(header, "header");
            Objects.requireNonNull(payload, "payload");
            return new MultipartDescriptionResponse(header, payload);
        }
    }

}
