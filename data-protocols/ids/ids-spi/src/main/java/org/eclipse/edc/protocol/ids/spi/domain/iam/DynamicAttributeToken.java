/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.protocol.ids.spi.domain.iam;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;

import java.util.Objects;

/**
 * Token format as specified by IDS.
 * .cf https://industrialdataspace.jiveon.com/docs/DOC-2524
 */
@JsonDeserialize(builder = DynamicAttributeToken.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicAttributeToken {
    private static final String ID_BASE = "https://w3id.org/idsa/autogen/dynamicAttributeToken/";

    @JsonProperty("@context")
    private String context = IdsConstants.CONTEXT;

    @JsonProperty("@type")
    private String type = "ids:DynamicAttributeToken";

    @JsonProperty("@id")
    private String id;

    @JsonProperty("ids:tokenValue")
    private String tokenValue;

    @JsonProperty("ids:tokenFormat")
    private TokenFormat tokenFormat = TokenFormat.JWT;

    private DynamicAttributeToken() {
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getContext() {
        return context;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public TokenFormat getTokenFormat() {
        return tokenFormat;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final DynamicAttributeToken token;

        private Builder() {
            token = new DynamicAttributeToken();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("@context")
        public Builder context(String context) {
            token.context = context;
            return this;
        }

        @JsonProperty("@id")
        public Builder id(String id) {
            token.id = id;
            return this;
        }

        public Builder relativeId(String id) {
            token.id = ID_BASE + id;
            return this;
        }

        @JsonProperty("@type")
        public Builder type(String type) {
            token.type = type;
            return this;
        }

        @JsonProperty("ids:tokenValue")
        public Builder tokenValue(String tokenValue) {
            token.tokenValue = tokenValue;
            return this;
        }

        @JsonProperty("ids:tokenFormat")
        public Builder tokenFormat(TokenFormat tokenFormat) {
            token.tokenFormat = tokenFormat;
            return this;
        }

        public DynamicAttributeToken build() {
            Objects.requireNonNull(token.id, "Property 'id' must be specified");
            Objects.requireNonNull(token.tokenValue, "Property 'tokenValue' must be specified");
            Objects.requireNonNull(token.tokenFormat, "Property 'tokenFormat' must be specified");
            return token;
        }
    }

}
