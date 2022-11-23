/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.concurrent.TimeUnit;

@JsonDeserialize(builder = DurationDto.Builder.class)
public class DurationDto {

    @Positive(message = "value must be positive")
    private long value;
    @NotBlank(message = "unit cannot be blank")
    private String unit;

    private DurationDto() {
    }

    public long getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    @JsonIgnore
    public long toSeconds() {
        return TimeUnit.valueOf(unit).toSeconds(value);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final DurationDto dto;

        private Builder() {
            this.dto = new DurationDto();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder unit(String unit) {
            dto.unit = unit;
            return this;
        }

        public Builder value(long value) {
            dto.value = value;
            return this;
        }

        public DurationDto build() {
            return dto;
        }
    }
}
