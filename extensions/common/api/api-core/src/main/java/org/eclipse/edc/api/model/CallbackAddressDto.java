/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO for {@link CallbackAddress}
 */
@JsonDeserialize(builder = CallbackAddressDto.Builder.class)
public class CallbackAddressDto extends BaseDto {

    @NotBlank(message = "uri is mandatory")
    private String uri;

    @NotNull(message = "events cannot be null")
    private Set<String> events = new HashSet<>();

    private boolean transactional;

    private String authKey;
    private String authCodeId;

    private CallbackAddressDto() {

    }

    public boolean isTransactional() {
        return transactional;
    }

    public Set<String> getEvents() {
        return events;
    }

    public String getUri() {
        return uri;
    }

    public String getAuthCodeId() {
        return authCodeId;
    }

    public String getAuthKey() {
        return authKey;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final CallbackAddressDto dto;

        private Builder() {
            this.dto = new CallbackAddressDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }


        public Builder uri(String url) {
            dto.uri = url;
            return this;
        }

        public Builder events(Set<String> events) {
            dto.events = events;
            return this;
        }

        public Builder transactional(boolean transactional) {
            dto.transactional = transactional;
            return this;
        }

        public Builder authKey(String authKey) {
            dto.authKey = authKey;
            return this;
        }

        public Builder authCodeId(String authCodeId) {
            dto.authCodeId = authCodeId;
            return this;
        }
        
        public CallbackAddressDto build() {
            return dto;
        }

    }
}
