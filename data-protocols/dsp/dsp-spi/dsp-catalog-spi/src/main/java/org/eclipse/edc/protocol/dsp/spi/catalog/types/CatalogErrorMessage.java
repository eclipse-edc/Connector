/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.edc.protocol.dsp.spi.catalog.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonTypeName("dspace:CatalogErrorMessage")
@JsonDeserialize(builder = CatalogErrorMessage.Builder.class)
public class CatalogErrorMessage {
    
    private String code;
    
    private List<String> reasons;
    
    @JsonProperty("dspace:code")
    public String getCode() {
        return code;
    }
    
    @JsonProperty("dspace:reason")
    public List<String> getReasons() {
        return reasons;
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final CatalogErrorMessage error;
        
        private Builder() {
            error = new CatalogErrorMessage();
        }
        
        public static Builder newInstance() {
            return new Builder();
        }
        
        public Builder code(String code) {
            error.code = code;
            return this;
        }
        
        public Builder reasons(List<String> reasons) {
            error.reasons = reasons;
            return this;
        }
        
        public Builder reason(String reason) {
            if (error.reasons == null) {
                error.reasons = new ArrayList<>();
            }
            error.reasons.add(reason);
            return this;
        }
        
        public CatalogErrorMessage builder() {
            Objects.requireNonNull(error, "Code must not be null");
            return error;
        }
    }
}
