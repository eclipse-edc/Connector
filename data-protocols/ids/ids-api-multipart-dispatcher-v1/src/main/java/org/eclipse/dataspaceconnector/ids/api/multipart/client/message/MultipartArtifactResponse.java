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
import de.fraunhofer.iais.eis.ResponseMessage;
import org.jetbrains.annotations.Nullable;

//TODO define return type
public class MultipartArtifactResponse implements MultipartResponse<String> {

    private ResponseMessage header;

    @Nullable
    private String payload;

    private MultipartArtifactResponse() { }

    @Override
    public Message getHeader() {
        return header;
    }

    @Override
    public @Nullable String getPayload() {
        return payload;
    }

    public static class Builder {
        private final MultipartArtifactResponse artifactResponse;

        private Builder() {
            this.artifactResponse = new MultipartArtifactResponse();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder header(ResponseMessage header) {
            this.artifactResponse.header = header;
            return this;
        }

        public Builder payload(@Nullable String payload) {
            this.artifactResponse.payload = payload;
            return this;
        }

        public MultipartArtifactResponse build() {
            Objects.requireNonNull(artifactResponse.header, "header");
            return artifactResponse;
        }
    }

}
