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

package org.eclipse.edc.protocol.dsp.catalog.spi.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.query.QuerySpec;

/**
 * Models a catalog request message as defined in the dataspace protocol specification.
 */
@JsonDeserialize(builder = CatalogRequestMessage.Builder.class)
public class CatalogRequestMessage {
    
    private QuerySpec filter;
    
    public QuerySpec getFilter() {
        return filter;
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final CatalogRequestMessage request;
        
        private Builder() {
            request = new CatalogRequestMessage();
        }
        
        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
        
        public Builder filter(QuerySpec filter) {
            request.filter = filter;
            return this;
        }
        
        public CatalogRequestMessage build() {
            return request;
        }
    }
    
}
