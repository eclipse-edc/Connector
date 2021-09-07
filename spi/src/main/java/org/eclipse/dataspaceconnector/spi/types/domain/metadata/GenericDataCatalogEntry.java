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

package org.eclipse.dataspaceconnector.spi.types.domain.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Generic extension properties.
 */
@JsonTypeName("dataspaceconnector:genericdataentryextensions")
@JsonDeserialize(builder = GenericDataCatalogEntry.Builder.class)
public class GenericDataCatalogEntry implements DataCatalogEntry {
    private final Map<String, Object> properties = new HashMap<>();

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public DataAddress getAddress() {
        return DataAddress.Builder.newInstance()
                //convert a Map of String-Object to String-String
                .properties(Optional.ofNullable(getProperties()).orElseGet(Collections::emptyMap).entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toString())))
                .build();

    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final GenericDataCatalogEntry lookup;

        private Builder() {
            lookup = new GenericDataCatalogEntry();
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder property(String key, String value) {
            lookup.properties.put(key, value);
            return this;
        }

        public GenericDataCatalogEntry build() {
            return lookup;
        }
    }


}
