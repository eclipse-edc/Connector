/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

@JsonDeserialize(builder = IdResponseDto.Builder.class)
public class IdResponseDto extends BaseResponseDto {

    public static final String EDC_ID_RESPONSE_DTO_TYPE = EDC_NAMESPACE + "IdResponseDto";
    public static final String EDC_ID_RESPONSE_DTO_CREATED_AT = EDC_NAMESPACE + "createdAt";

    private String id;

    public IdResponseDto() {
    }

    public String getId() {
        return id;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends BaseResponseDto.Builder<IdResponseDto, Builder> {
        private Builder() {
            super(new IdResponseDto());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder id(String id) {
            dto.id = id;
            return this;
        }
    }
}
