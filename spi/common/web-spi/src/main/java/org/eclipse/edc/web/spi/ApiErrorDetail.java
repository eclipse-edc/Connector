/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.web.spi;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Define the structure for an API error to be returned to the client
 */
public class ApiErrorDetail {

    @JsonProperty
    private String message;
    @JsonProperty
    private String type;
    @JsonProperty
    private String path;
    @JsonProperty
    private Object invalidValue;

    private ApiErrorDetail() {

    }

    /**
     * A description message about the error
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * The type of the error occurred
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * The path of the field that caused the error.
     * This value can be null if the error it's not field-specific
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * The value of the field that caused the error.
     * This value can be null if the error it's not field-specific
     *
     * @return the invalid value
     */
    public Object getInvalidValue() {
        return invalidValue;
    }

    public static class Builder {

        private final ApiErrorDetail apiError = new ApiErrorDetail();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder message(String message) {
            apiError.message = message;
            return this;
        }

        public Builder type(String type) {
            apiError.type = type;
            return this;
        }

        public Builder path(String path) {
            apiError.path = path;
            return this;
        }

        public Builder value(Object value) {
            apiError.invalidValue = value;
            return this;
        }

        public ApiErrorDetail build() {
            return apiError;
        }
    }
}
