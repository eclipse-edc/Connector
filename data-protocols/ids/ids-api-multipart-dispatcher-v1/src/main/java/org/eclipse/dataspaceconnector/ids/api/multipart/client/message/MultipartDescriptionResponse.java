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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.ModelClass;
import de.fraunhofer.iais.eis.ResponseMessage;

public class MultipartDescriptionResponse implements MultipartResponse<ModelClass> {

    private ResponseMessage header;

    private ModelClass payload;

    private MultipartDescriptionResponse() { }

    @Override
    public Message getHeader() {
        return header;
    }

    @Override
    public ModelClass getPayload() {
        return payload;
    }

    public static class Builder {
        private final MultipartDescriptionResponse descriptionResponse;

        private Builder() {
            this.descriptionResponse = new MultipartDescriptionResponse();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder header(ResponseMessage header) {
            this.descriptionResponse.header = header;
            return this;
        }

        public Builder payload(ModelClass payload) {
            this.descriptionResponse.payload = payload;
            return this;
        }

        public MultipartDescriptionResponse build() {
            Objects.requireNonNull(descriptionResponse.header, "header");
            Objects.requireNonNull(descriptionResponse.payload, "payload");
            return descriptionResponse;
        }
    }

}
