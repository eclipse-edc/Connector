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

package org.eclipse.dataspaceconnector.tooling.module.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A configuration point that can be set for an {@link EdcModule}.
 */
@JsonDeserialize(builder = ConfigurationSetting.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationSetting {
    private String key;
    private boolean required = true;
    private String type = "string";
    private String pattern;
    private Long minimum;
    private Long maximum;
    private String description;

    /**
     * Returns the normalized configuration key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns true if this configuration must be supplied or false if it is optional.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Returns the value type corresponding to Json schema types.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns a REGEX expressing the pattern required by the configuration value or null if not specififed.
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Returns a minimum valid value for numeric configuration or null if not specified.
     */
    public Long getMinimum() {
        return minimum;
    }

    /**
     * Returns a maximum valid value for numeric configuration or null if not specified.
     */
    public Long getMaximum() {
        return maximum;
    }

    /**
     * Returns a description of this configuration element.
     */
    public String getDescription() {
        return description;
    }

    private ConfigurationSetting() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ConfigurationSetting) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private ConfigurationSetting setting;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder required(boolean required) {
            setting.required = required;
            return this;
        }

        public Builder key(String key) {
            setting.key = key;
            return this;
        }

        public Builder type(String type) {
            setting.type = type;
            return this;
        }

        public Builder pattern(String pattern) {
            setting.pattern = pattern;
            return this;
        }

        public Builder minimum(Long minimum) {
            setting.minimum = minimum;
            return this;
        }

        public Builder maximum(Long maximum) {
            setting.maximum = maximum;
            return this;
        }

        public Builder description(String description) {
            setting.description = description;
            return this;
        }


        public ConfigurationSetting build() {
            requireNonNull(setting.key, "key");
            return setting;
        }

        private Builder() {
            setting = new ConfigurationSetting();
        }
    }
}
