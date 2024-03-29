/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.api.transferprocess.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = TransferProcessFailStateDto.Builder.class)
public class TransferProcessFailStateDto {
    private String errorMessage;

    private TransferProcessFailStateDto() {

    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final TransferProcessFailStateDto request;

        private Builder() {
            request = new TransferProcessFailStateDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder errorMessage(String errorMessage) {
            request.errorMessage = errorMessage;
            return this;
        }

        public TransferProcessFailStateDto build() {
            return request;
        }
    }
}
