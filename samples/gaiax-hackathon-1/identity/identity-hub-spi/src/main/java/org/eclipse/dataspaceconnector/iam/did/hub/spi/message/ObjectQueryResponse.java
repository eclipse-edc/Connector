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
package org.eclipse.dataspaceconnector.iam.did.hub.spi.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@JsonTypeName("ObjectQueryResponse")
@JsonDeserialize(builder = ObjectQueryResponse.Builder.class)
public class ObjectQueryResponse extends HubMessage {
    private String developerMessage;
    private String skipToken;

    private List<HubObject> objects = new ArrayList<>();

    private ObjectQueryResponse() {
    }

    @JsonProperty("developer_message")
    public String getDeveloperMessage() {
        return developerMessage;
    }

    public List<HubObject> getObjects() {
        return objects;
    }

    public String getSkipToken() {
        return skipToken;
    }

    public static class Builder extends HubMessage.Builder {
        private ObjectQueryResponse response;

        @JsonCreator()
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("developer_message")
        public Builder developerMessage(String message) {
            this.response.developerMessage = message;
            return this;
        }

        public Builder objects(List<HubObject> objects) {
            this.response.objects.addAll(objects);
            return this;
        }

        public Builder object(HubObject object) {
            this.response.objects.add(object);
            return this;
        }

        public Builder skipToken(String skipToken) {
            this.response.skipToken = skipToken;
            return this;
        }

        public ObjectQueryResponse build() {
            return response;
        }

        private Builder() {
            response = new ObjectQueryResponse();
        }

    }
}
