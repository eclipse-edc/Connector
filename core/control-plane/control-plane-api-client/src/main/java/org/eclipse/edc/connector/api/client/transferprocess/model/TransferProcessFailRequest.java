/*
 *  Copyright (c) 2020, 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.api.client.transferprocess.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Payload for notifying a failed transfer
 */
@JsonDeserialize(builder = TransferProcessFailRequest.Builder.class)
public class TransferProcessFailRequest {
    private String errorMessage;

    private TransferProcessFailRequest() {

    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final TransferProcessFailRequest request;

        private Builder() {
            request = new TransferProcessFailRequest();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder errorMessage(String errorMessage) {
            request.errorMessage = errorMessage;
            return this;
        }

        public TransferProcessFailRequest build() {
            return request;
        }
    }
}
